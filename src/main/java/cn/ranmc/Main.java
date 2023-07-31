package cn.ranmc;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    @Getter
    private String prefix;
    //计时器
    private BukkitTask task;
    //TPS
    private Double tps = 19.99;
    private Double tpsCheck = 0.0;
    //最新版本
    private String lastestVersion = "检查失败";
    //生物堆叠器
    private List<String> stackerList;
    //限制生物过多
    private Map<String, Integer> mob = new HashMap<>();
    private int spawnTime = 1;
    //红石高频
    private Map<Location, Long> redstone = new HashMap<>();
    private Map<Location, Integer> warning = new HashMap<>();
    //不会限制的生成方式
    private static List<CreatureSpawnEvent.SpawnReason> REASONS = Arrays.asList(
            new CreatureSpawnEvent.SpawnReason[]{
                    CreatureSpawnEvent.SpawnReason.CUSTOM,
                    CreatureSpawnEvent.SpawnReason.COMMAND,
                    CreatureSpawnEvent.SpawnReason.SPAWNER_EGG,
                    CreatureSpawnEvent.SpawnReason.CURED
            });

    @Override
    public void onEnable() {
        outPut("&e-----------------------");
        outPut("&b性能优化器 &dBy阿然");
        outPut("&b插件版本:"+getDescription().getVersion());
        outPut("&b服务器版本:"+getServer().getVersion());
        outPut("&cQQ 2263055528");
        outPut("&e-----------------------");

        //加载数据
        loadConfig();

        //检查更新
        //updateCheck();

        //注册EVENT
        Bukkit.getPluginManager().registerEvents(this, this);

        //计时器
        task = Bukkit.getScheduler().runTaskTimer(this, () -> {
            warning = new HashMap<>();

            if (spawnTime > 0) spawnTime--;
            if (spawnTime == 0) {
                spawnTime = getConfig().getInt("spawnTime", 30);
                mob = new HashMap<>();
            }

            try {
                tps = Double.parseDouble(PlaceholderAPI.setPlaceholders(null, "%server_tps_1%"));
            } catch (Exception e) {
                //e.printStackTrace();
            }

            if (tps >= tpsCheck) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Entity[] entities = player.getLocation().getChunk().getEntities();
                    for (int i = 0; i < entities.length; i++) {
                        if (stackerList.contains(entities[i].getType().name())) {
                            String name = entities[i].getCustomName();
                            if (name != null && name.contains(color("&cx"))) {
                                int count = 0;
                                try {
                                    count += Integer.parseInt(entities[i].getCustomName().replace(color("&cx"), ""));
                                } catch (NumberFormatException e) {
                                }
                                for (int ii = 1; ii < count; ii++) {
                                    Location location = entities[i].getLocation();
                                    location.getWorld().spawnEntity(location, entities[i].getType());
                                }
                            }
                            entities[i].setCustomName(null);
                        }
                    }
                }
            }
        }, 0, 20 * 60);

        super.onEnable();
    }

    @Override
    public void onDisable() {
        task.cancel();
        super.onDisable();
    }


    /**
     * 雪球刷怪塔
     * @param event
     */
    @EventHandler
    public void onProjectileLaunchEvent(ProjectileLaunchEvent event) {
        if (getConfig().getBoolean("snowball", true) && event.getEntityType() == EntityType.SNOWBALL)
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> event.getEntity().remove() , getConfig().getInt("snowballDisappearDely", 60));
    }

    @EventHandler
    public void onEntityPlaceEvent(EntityPlaceEvent event) {
        Entity en = event.getEntity();
        EntityType et = event.getEntityType();
        // 限制船过多
        if (getConfig().getBoolean("boatLimit", true) && et.toString().contains("BOAT")) {
            Entity[] entities = en.getLocation().getChunk().getEntities();
            int liveCount = 0;
            for (int i = 0; i < entities.length; i++) {
                if (entities[i].getType().toString().contains("BOAT")) {
                    liveCount++;
                }
            }
            if (liveCount >= getConfig().getInt("chunkBoatLimit")) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 生物生成时间
     * @param event
     */

    @EventHandler
    public void onEntitySpawnEvent(CreatureSpawnEvent event) {
        Entity en = event.getEntity();
        if (event.isCancelled() || en == null) return;

        // 限制刷怪笼
        if (getConfig().getBoolean("spawner") && event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER)) {
            int i = (int) (Math.random() * 100);
            if (i > getConfig().getInt("spawnerChange")){
                event.setCancelled(true);
                return;
            }
        }

        EntityType et = event.getEntityType();

        // 限制生物过多
        if (getConfig().getBoolean("mob") && !REASONS.contains(event.getSpawnReason())) {
            List<String> spawnList = getConfig().getStringList("mobList");
            if (spawnList.contains(et.toString())) {
                int num = (int) (Math.random() * 100);
                if (num > getConfig().getInt("spawnChange")) {
                    event.setCancelled(true);
                } else {
                    Location loc = en.getLocation();
                    int lx = loc.getBlockX() / 100;
                    int lz = loc.getBlockZ() / 100;
                    if (loc.getBlockX() < 0) {
                        lx--;
                    }
                    if (loc.getBlockZ() < 0) {
                        lz--;
                    }

                    Entity[] entities = en.getLocation().getChunk().getEntities();
                    int liveCount = 0;
                    for (int i = 0; i < entities.length; i++) {
                        if (entities[i].getType() == et) {
                            liveCount++;
                        }
                    }
                    if (liveCount >= getConfig().getInt("chunkLimit")) {
                        event.setCancelled(true);
                        return;
                    }

                    String name = lx + loc.getWorld().getName() + lz + et;
                    int count = 0;
                    if (mob.containsKey(name)) count = mob.get(name);
                    if (count >= getConfig().getInt("spawnLimit")) {
                        event.setCancelled(true);
                    } else {
                        count++;
                        mob.put(name, count);
                    }
                }
            }

        }

        //生物堆叠器
        if (getConfig().getBoolean("stacker") && tps < tpsCheck) {
            if (stackerList.contains(et.toString())) {
                Entity[] entities = en.getLocation().getChunk().getEntities();
                if (entities.length==0) return;
                int liveCount = 0;
                int log = 0;
                int count = 0;
                int base = 0;
                for (int i = 0; i < entities.length; i++) {
                    if (et.equals(entities[i].getType())) {
                        String name = entities[i].getCustomName();
                        if (name==null) {
                            liveCount++;
                            if (liveCount > 1) {
                                entities[i].remove();
                                count++;
                            } else if (liveCount==1) log = i;
                        } else if (name.contains(color("&cx"))) base = i;
                    }
                }
                if (base==0) {
                    count++;
                    if (!entities[log].isDead() && count>1) entities[log].setCustomName(color("&cx")+count);
                } else {
                    int baseCount = 0;
                    try {
                        baseCount += Integer.parseInt(entities[base].getCustomName().replace(color("&cx"),""));
                    } catch (NumberFormatException e) {
                    }
                    count += baseCount;
                    if (!entities[base].isDead() && count>1) entities[base].setCustomName(color("&cx")+count);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event){
        //生物堆叠器分离
        if (getConfig().getBoolean("stacker")) {
            Entity entity = event.getEntity();
            String name = entity.getCustomName();
            if (stackerList.contains(event.getEntityType().toString()) && name!=null && name.contains(color("&cx"))) {
                int count = 0;
                try {
                    count += Integer.parseInt(name.replace(color("&cx"),""));
                } catch (NumberFormatException e) {
                }
                if (count>1) {
                    count--;
                    Location location = entity.getLocation();
                    LivingEntity newMob = (LivingEntity) location.getWorld().spawnEntity(location, entity.getType());
                    if (count>1) newMob.setCustomName(color("&cx")+count);
                }
            }
        }

    }

    @EventHandler
    public void onPistonExtendEvent(BlockPistonExtendEvent event) {
        if (getConfig().getBoolean("observerPiston", false)) {
            for (Block block : event.getBlocks()) {
                if (block.getType() == Material.OBSERVER) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        redstoneCheck(event.getBlock().getLocation());


    }

    private void removeRedstoneBlock(Block block) {
        block.setType(Material.AIR);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            block.setType(Material.AIR);
        });
    }

    private void redstoneCheck(Location loc) {
        if (getConfig().getBoolean("redstoneClock") &&
                redstone.containsKey(loc) &&
                !getConfig().getStringList("redstoneDisabledWorld").contains(loc.getWorld().getName())) {
            long time = System.currentTimeMillis() - redstone.get(loc);
            long redstoneDely = getConfig().getInt("redstoneDely", 500);
            if (time > 1 && time < redstoneDely) {
                if (warning.containsKey(loc)) {
                    int count = warning.get(loc) + 1;
                    if (count >= getConfig().getInt("redstoneHold", 10)) {
                        warning.remove(loc);
                        redstone.remove(loc);
                        removeRedstoneBlock(loc.getBlock());
                        if (getConfig().getBoolean("redstoneLightning", true)) loc.getWorld().strikeLightningEffect(loc);
                        if (getConfig().getBoolean("redstoneWarning", true)) outPut("&c检测到" + time + "ms红石高频 " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
                    } else {
                        warning.put(loc, count);
                    }
                } else {
                    warning.put(loc, 1);
                }
            } else if (time > (redstoneDely * getConfig().getInt("redstoneHold", 20))) {
                warning.remove(loc);
            }
        }
        redstone.put(loc, System.currentTimeMillis());
    }

    /**
     * 限制红石
     * @param event
     */
    @EventHandler
    public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
        if (getConfig().getBoolean("redstone")) {
            redstoneCheck(event.getBlock().getLocation());

            if (tps < 16 && event.getBlock().getLocation().getBlockY() > 200) {
                event.setNewCurrent(0);
                return;
            }
            if (tps < 14 && event.getBlock().getLocation().getBlockY() > 150) {
                event.setNewCurrent(0);
                return;
            }
            if (tps < 12 && event.getBlock().getLocation().getBlockY() > 100) {
                event.setNewCurrent(0);
                return;
            }
            if (tps < 10) {
                event.setNewCurrent(0);
                return;
            }
        }

    }

    /**
     * 指令输入
     * @param sender
     * @param cmd
     * @param label
     * @param args
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("ro")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")){
                if (sender.hasPermission("ro.admin")) {
                    loadConfig();
                    sender.sendMessage(prefix + color("&a重载成功"));
                    return true;
                } else {
                    sender.sendMessage(prefix + color("&c没有权限"));
                }
            }
        }

        if (!(sender instanceof Player)) {
            outPut("&c该指令不能在控制台输入");
            return true;
        }

        //Player player = (Player) sender;

        sender.sendMessage("未知指令");
        return true;
    }

    /**
     * 加载配置文件
     */
    private void loadConfig(){
        //加载config
        if (!new File(getDataFolder() + File.separator + "config.yml").exists()) {
            saveDefaultConfig();
        }
        reloadConfig();

        prefix = color(getConfig().getString("prefix", "&b性能优化器>>>"));

        stackerList = getConfig().getStringList("stackerList");
        tpsCheck = getConfig().getDouble("tpsCheck");
    }

    /**
     * 执行指令
     * @param command
     * @return
     */
    /*
    public void run(String command) {
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }*/

    /**
     * 文本颜色
     * @param text
     * @return
     */
    private static String color(String text){
        return text.replace("&","§");
    }

    /**
     * 后台信息
     * @param msg
     */
    public void outPut(String msg){
        Bukkit.getConsoleSender().sendMessage(color(msg));
    }

    /**
     * 公屏信息
     * @param msg
     */
    /*
    public void say(String msg){
        Bukkit.broadcastMessage(color(msg));
    }*/

    /**
     * 检查更新
     */
    public void updateCheck() {
        try {
            URL url = new URL("https://www.ranmc.cn/plugins/ranOptimiser.txt");
            InputStream is = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            lastestVersion = br.readLine();
            if (getDescription().getVersion().equals(lastestVersion)) {
                outPut(prefix + color("&a当前已是最新版本"));
            } else {
                outPut(prefix + color("&c检测到新版本" + lastestVersion));
            }
        } catch (Exception e) {
            outPut(prefix + color("&c检查更新失败"));
        }

    }
}
