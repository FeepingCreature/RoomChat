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
    // Uncomment as needed.
    //etc.getLoader().addListener( PluginLoader.Hook.ARM_SWING, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.BLOCK_CREATED, l, this, PluginListener.Priority.MEDIUM);
    etc.getLoader().addListener( PluginLoader.Hook.BLOCK_BROKEN, l, this, PluginListener.Priority.MEDIUM);
    etc.getLoader().addListener( PluginLoader.Hook.CHAT, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.COMMAND, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.COMPLEX_BLOCK_CHANGE, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.COMPLEX_BLOCK_SEND, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.DISCONNECT, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.INVENTORY_CHANGE, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.IPBAN, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.KICK, l, this, PluginListener.Priority.MEDIUM);
    // etc.getLoader().addListener( PluginLoader.Hook.LOGIN, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.LOGINCHECK, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.NUM_HOOKS, l, this, PluginListener.Priority.MEDIUM);
    etc.getLoader().addListener( PluginLoader.Hook.PLAYER_MOVE, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.SERVERCOMMAND, l, this, PluginListener.Priority.MEDIUM);
    //etc.getLoader().addListener( PluginLoader.Hook.TELEPORT, l, this, PluginListener.Priority.MEDIUM);
  }

  // Sends a message to all players!
  public void broadcast(String message) {
    for (Player p : etc.getServer().getPlayerList()) {
      p.sendMessage(message);
    }
  }

  public class Listener extends PluginListener {
    RoomChat rc;
    
    // This controls the accessability of functions / variables from the main class.
    public Listener(RoomChat rc) { this.rc = rc; ; }
    // remove the /* and */ from any function you want to use
    // make sure you add them to the listener above as well!
    
    public void onPlayerMove(Player player, Location from, Location to) {
      int x = (int) to.x, y = (int) to.y, z = (int) to.z;
      IntVec pos = new IntVec(x, y, z);
      Room room = findRoom(pos);
      Room playerroom = room_map.get(player);
      if (null != room) {
        if (room == playerroom) return;
        player.sendMessage("You have entered the room \"" + room.name + "\". ");
        room_map.put(player, room);
        return;
      } else if (room != playerroom) { // don't return; still have to run stochastic search
        player.sendMessage("You have left the room \"" + playerroom.name + "\". ");
        room_map.remove(player);
        return;
      }
      // stochastic room search
      Sign sign = null;
      for (int i = 0; i < 512; ++i) { // adapt this if things get too slow for some reason
        int checkx = x + rc.getRange(-8, 8), checky = y + rc.getRange(-1, 4), checkz = z + rc.getRange(-8, 8);
        ComplexBlock block = etc.getServer().getComplexBlock(checkx, checky, checkz);
        if (!(block instanceof Sign))
          continue;
        sign = (Sign) block;
      }
      if (null == sign) return;
      IntVec signpos = new IntVec(sign.getX(), sign.getY(), sign.getZ());
      if (!sign.getText(0).equalsIgnoreCase("[Room]")) return;
      if (null != findRoom(signpos)) // already registered
        return;
      String roomName = sign.getText(1);
      // grow
      float air_ratio = 0.7f; // required ratio of air in added space
      Area ar = new Area(signpos, new IntVec(signpos.x + 1, signpos.y + 1, signpos.z + 1));
      int oldvol = 1, oldair = 0; // one volume, occupied by sign :p
      int trygrow = 0; // direction
      while (true) {
        Area nar = ar.grow(trygrow ++);
        int vol = nar.volume(), air = nar.checkAirRate();
        int volchg = vol - oldvol, airchg = air - oldair;
        assert(volchg > 0);
        float ratio = airchg * 1.0f / volchg;
        // player.sendMessage("info: vol [" + oldvol + " + " + volchg + " = " + vol + "], air [" + oldair + " + " + airchg + " = " + air + "], ratio " + ratio + " after " + trygrow);
        if (ratio > air_ratio) { // success!
          ar = nar; oldvol = vol; oldair = air;
          if (vol > 16*16*16) break; // room size limit! TODO: make variable
          trygrow = 0;
          continue;
        }
        if (trygrow == 6) break; // can't find any more directions to grow in
      }
      // player.sendMessage("Test: " + ar.volume() + ", " + ar.checkAirRate() + ", ar is " + ar);
      // register room
      player.sendMessage("Found a room: \"" + roomName + "\", sized " + ar.size());
      rc.rooms.add(new Room(ar, roomName, sign));
      // retry
      onPlayerMove(player, from, to);
    }

    /*
    public boolean onTeleport(Player player, Location from, Location to) {
    return false;
    }
    */

    /*
    public String onLoginChecks(String user) {
    return null;
    }
    */

    /*public void onLogin(Player player) {
      // Player Message
      // player.sendMessage(Colors.Yellow + "Currently running plugin: " + p.name + " v" + p.version + "!");

      // Global Message
      // p.broadcast(Colors.Green + player.getName() + " has joined the server! Wooo~");
    }*/

    /*
    public void onDisconnect(Player player) {
    }
    */

    public boolean onChat(Player player, String message) {
      Room room = rc.room_map.get(player);
      if (null != room) {
        for (Player targetplayer: room.findPlayers()) {
          targetplayer.sendMessage("<" + player.getColor() + player.getName() + Colors.Gold + " [" + room.name + "]" + Colors.White + "> " + message);
        }
        return true;
      }
      return false;
    }

    /*
    public boolean onCommand(Player player, String[] split) {
    return false;
    }
    */

    /*
    public boolean onConsoleCommand(String[] split) {
    return false;
    }
    */

    /*
    public void onBan(Player mod, Player player, String reason) {
    }
    */

    /*
    public void onIpBan(Player mod, Player player, String reason) {
    }
    */

    /*
    public void onKick(Player mod, Player player, String reason) {
    }
    */

    /*
    public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
    return false;
    }
    */
    public boolean onBlockBreak(Player player, Block block) {
      int idx = 0, erase = -1;
      for (Room room: rooms) {
        if (new Pos<Sign>(room.sign).equals(new Pos<Block>(block))) {
          erase = idx;
          break;
        }
        idx ++;
      }
      if (erase != -1) {
        player.sendMessage("You have destroyed the room \"" + rooms.get(erase).name + "\". ");
        rooms.removeElementAt(erase);
      }
      return false;
    }

    /*
    public void onArmSwing(Player player) {
    }
    */

    /*
    public boolean onInventoryChange(Player player) {
    return false;
    }
    */

    /*
    public boolean onComplexBlockChange(Player player, ComplexBlock block) {
    return false;
    }
    */

    /*
    public boolean onSendComplexBlock(Player player, ComplexBlock block) {
    return false;
    }
    */
  }
}

