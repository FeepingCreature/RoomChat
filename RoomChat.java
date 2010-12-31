import java.util.logging.Logger;
import java.util.*;
import java.lang.reflect.*;

public class RoomChat extends Plugin {
  private Listener l = new Listener(this);
  protected static final Logger log = Logger.getLogger("Minecraft");
  private String name = "RoomChat";
  private String version = "0.1";
  private Random rng = new Random(1);
  private int getRange(int from, int to) { return rng.nextInt(to - from) + from; }
  
  private class IntVec {
    int x, y, z;
    private IntVec() { }
    IntVec(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    IntVec(IntVec iv) { x = iv.x; y = iv.y; z = iv.z; }
    public String toString() { return String.format("<%s, %s, %s>", x, y, z); }
    public boolean equals(IntVec iv) { return x == iv.x && y == iv.y && z == iv.z; }
  }

  private class Area {
    IntVec from, to;
    Area(IntVec f, IntVec t) { from = f; to = t; }
    Area(int a, int b, int c, int d, int e, int f) {
      // assert(from < to);
      int temp;
      if (d < a) { temp = a; a = d; d = temp; }
      if (e < b) { temp = b; b = e; e = temp; }
      if (f < c) { temp = c; c = f; f = temp; }
      
      from = new IntVec(a, b, c);
      to = new IntVec(d, e, f);
    }
    public boolean isInside(IntVec iv) {
      return
        (iv.x >= from.x && iv.x <= to.x) &&
        (iv.y >= from.y && iv.y <= to.y) &&
        (iv.z >= from.z && iv.z <= to.z);
    }
    public IntVec size() { return new IntVec(to.x - from.x, to.y - from.y, to.z - from.z); }
    public int volume() { IntVec sz = size(); return sz.x * sz.y * sz.z; }
    // return how many blocks are not air
    public int checkAirRate() {
      Server serv = etc.getServer();
      int res = 0;
      for (int z = from.z; z < to.z; ++z) {
        for (int y = from.y; y < to.y; ++y) {
          for (int x = from.x; x < to.x; ++x) {
            int id = serv.getBlockIdAt(x, y, z);
            if (id == Block.Type.Air.ordinal())
              res ++;
          }
        }
      }
      return res;
    }
    Area grow(int dir) {
      Area res = new Area(new IntVec(from), new IntVec(to));
      switch (dir) {
        case 0: res.from.x --; break;
        case 1: res.from.y --; break;
        case 2: res.from.z --; break;
        case 3: res.to.x ++; break;
        case 4: res.to.y ++; break;
        case 5: res.to.z ++; break;
      }
      return res;
    }
    public String toString() { return String.format("{%s - %s}", from, to); }
  }
  public class Pos<T> extends IntVec {
    public Pos(T t) {
      try {
        // THE POWER! THE POWER!! AHAHAHAAAA
        Class<T> cl = (Class<T>)t.getClass();
        Method gx = cl.getMethod("getX"), gy = cl.getMethod("getY"), gz = cl.getMethod("getZ");
        Object x = gx.invoke(t), y = gy.invoke(t), z = gz.invoke(t);
        if (x instanceof Double) {
          this.x = (int) (double) (Double) x;
          this.y = (int) (double) (Double) y; // assumed the same
          this.z = (int) (double) (Double) z;
        } else {
          this.x = (Integer) x;
          this.y = (Integer) y;
          this.z = (Integer) z;
        }
      } catch (Exception ex) {
        assert(false);
      }
    }
  }
  private class Room {
    Area area;
    String name;
    Sign sign;
    Room(Area ar, String name, Sign sign) { area = ar; this.name = name; this.sign = sign; }
    public boolean isInside(IntVec iv) { return area.isInside(iv); }
    public Vector<Player> findPlayers() {
      Vector<Player> res = new Vector<Player> ();
      for (Player player: etc.getServer().getPlayerList()) {
        if (isInside(new Pos<Player>(player)))
          res.add(player);
      }
      return res;
    }
    public void updateSign() {
      String lastline = sign.getText(3);
      if (lastline.length() == 0 || lastline.toLowerCase().startsWith("population") || lastline.toLowerCase().startsWith("pop")) {
        sign.setText(3, String.format("pop. %s", findPlayers().size()));
        sign.update();
      }
    }
  }
  
  Vector<Room> rooms = new Vector<Room> ();
  
  HashMap<Player, Room> room_map = new HashMap<Player, Room> ();
  
  private Room findRoom(IntVec pos) {
    for (Room room : rooms) {
      if (room.isInside(pos)) return room;
    }
    return null;
  }
  
  public void enable() {
  }
  
  public void disable() {
  }
  
  public void initialize() {
    log.info(name + " " + version + " initialized");
    etc.getLoader().addListener( PluginLoader.Hook.BLOCK_BROKEN, l, this, PluginListener.Priority.MEDIUM);
    etc.getLoader().addListener( PluginLoader.Hook.CHAT,         l, this, PluginListener.Priority.MEDIUM);
    etc.getLoader().addListener( PluginLoader.Hook.PLAYER_MOVE,  l, this, PluginListener.Priority.MEDIUM);
    etc.getLoader().addListener( PluginLoader.Hook.BLOCK_PLACE,  l, this, PluginListener.Priority.MEDIUM);
  }

  // Sends a message to all players!
  public void broadcast(String message) {
    for (Player p : etc.getServer().getPlayerList()) {
      p.sendMessage(message);
    }
  }

  public class Listener extends PluginListener {
    RoomChat rc;
    int excitement = 200; // are we looking for any rooms?
    public Listener(RoomChat rc) { this.rc = rc; ; }
    // check player for room status
    // true if in a room, false otherwise
    private boolean considerPlayer(Player player) {
      IntVec pos = new Pos<Player> (player);
      Room room = findRoom(pos);
      Room playerroom = room_map.get(player);
      if (room != playerroom) {
        if (null != room) {
          player.sendMessage("You have entered the room \"" + room.name + "\". ");
          room_map.put(player, room);
          room.updateSign();
        } else {
          player.sendMessage("You have left the room \"" + playerroom.name + "\". ");
          room_map.remove(player);
          playerroom.updateSign();
        }
      }
      return room != null;
    }
    void considerSign(Sign sign, Player player) {
      IntVec signpos = new IntVec(sign.getX(), sign.getY(), sign.getZ());
      if (!sign.getText(0).equalsIgnoreCase("[Room]")) return;
      if (null != findRoom(signpos)) // already registered
        return;
      String roomName = sign.getText(1);
      // grow
      float air_ratio = 0.7f; // required ratio of air in added space
      Area ar = new Area(signpos, new IntVec(signpos.x + 1, signpos.y + 1, signpos.z + 1));
      int oldvol = 1, oldair = 0; // one volume, occupied by sign :p
      while (true) {
        boolean couldGrow = false;
        // try to go for balanced growth rate in all dirs
        for (int dir = 0; dir < 6; ++dir) {
          Area nar = ar.grow(dir);
          int vol = nar.volume(), air = nar.checkAirRate();
          int volchg = vol - oldvol, airchg = air - oldair;
          assert(volchg > 0);
          float ratio = airchg * 1.0f / volchg;
          if (ratio > air_ratio) { // success!
            ar = nar; oldvol = vol; oldair = air;
            if (vol < 16*16*16) // room size limit! TODO: make variable
              couldGrow = true;
            break;
          }
        }
        if (!couldGrow) break;
      }
      // register room
      player.sendMessage("Found a room: \"" + roomName + "\", sized " + ar.size());
      rc.rooms.add(new Room(ar, roomName, sign));
      // update players
      for (Player pl: etc.getServer().getPlayerList())
        considerPlayer(pl);
    }
    private void checkForSign(Player player, Server serv, int x, int y, int z) {
      int id = serv.getBlockIdAt(x, y, z);
      if (id != 63 /* signpost */ && id != 68 /* wallsign */)
        return;
      ComplexBlock block = serv.getComplexBlock(x, y, z);
      if (!(block instanceof Sign)) {
        player.sendMessage("Test: " + id + ", " + block);
        return;
      }
      considerSign((Sign) block, player);
    }
    public void onPlayerMove(Player player, Location from, Location to) {
      int x = (int) to.x, y = (int) to.y, z = (int) to.z;
      IntVec pos = new IntVec(x, y, z);
      if (considerPlayer(player))
        return; // already in a room, no need to check
      // stochastic room search
      Server serv = etc.getServer();
      for (int i = 0; i < excitement; ++i) { // adapt this if things get too slow for some reason
        int checkx = x + rc.getRange(-8, 8), checky = y + rc.getRange(-1, 4), checkz = z + rc.getRange(-8, 8);
        checkForSign(player, serv, checkx, checky, checkz);
      }
      if (excitement > 200) excitement /= 1.2;
    }
    public boolean onChat(Player player, String message) {
      Room room = rc.room_map.get(player);
      if (null != room) {
        if (message.startsWith(".") && !message.startsWith("..")) { // global
          for (Player targetplayer: etc.getServer().getPlayerList()) {
            targetplayer.sendMessage("<" + player.getColor() + player.getName() + Colors.White + "> " + message.substring(1));
          }
        } else {
          for (Player targetplayer: room.findPlayers()) {
            targetplayer.sendMessage("<" + player.getColor() + player.getName() + Colors.Gold + " [" + room.name + "]" + Colors.White + "> " + message);
          }
        }
        return true;
      }
      return false;
    }
    public boolean onBlockPlace(Player player, Block blockPlaced, Block blockClicked, Item itemInHand) {
      // no point; the sign isn't placed yet and so we don't have the complexBlock data
      // checkForSign(player, etc.getServer(), blockPlaced.getX(), blockPlaced.getY(), blockPlaced.getZ());
      excitement = 1500; // WHERE IS IT WHERE IS IT OMG
      return false;
    }
    public boolean onBlockBreak(Player player, Block block) {
      int idx = 0, erase = -1;
      for (Room room: rooms) {
        // log.info(String.format("is sign @%s == %s? ", new Pos<Sign>(room.sign), new Pos<Block>(block)));
        if (new Pos<Sign>(room.sign).equals(new Pos<Block>(block))) {
          erase = idx;
          break;
        }
        idx ++;
      }
      if (erase != -1) {
        player.sendMessage("You have destroyed the room \"" + rooms.get(erase).name + "\". ");
        rooms.removeElementAt(erase);
        // update
        Location loc = new Location(player.getX(), player.getY(), player.getZ());
        onPlayerMove(player, loc, loc);
      }
      return false;
    }
  }
}

