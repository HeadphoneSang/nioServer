package com.chbcraft.internals.components;

import com.chbcraft.exception.ExceptionPrinter;
import com.chbcraft.internals.base.LibrariesClassLoader;
import com.chbcraft.internals.components.entries.PluginEntry;
import com.chbcraft.internals.components.entries.config.Configuration;
import com.chbcraft.internals.components.enums.SectionName;
import com.chbcraft.internals.components.listen.Cancelable;
import com.chbcraft.internals.components.listen.HandlerList;
import com.chbcraft.internals.components.listen.RegisteredListener;
import com.chbcraft.internals.components.loader.Loader;
import com.chbcraft.internals.components.sysevent.Event;
import com.chbcraft.internals.components.utils.ConfigurationUtil;
import com.chbcraft.plugin.Plugin;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

public class FloatPluginManager implements PluginManager{
    private static final HashSet<String> disablePlugins = new HashSet<>();
    /**
     * 允许加载的最大插件数
     */
    private int MAX_PLUGIN = Integer.MAX_VALUE;
    /**
     * 插件进程管理器
     */
    protected final Loader processor;
    /**
     * 插件路径->?/?/plugins
     */
    protected String PLUGIN_PATH;
    /**
     * 消息盒子
     */
    protected final MessageBox logger;

    public LibrariesClassLoader libLoader;
    FloatPluginManager(String path){
        PLUGIN_PATH = FloatSphere.getRootPath()+path;
        processor = PluginProcessor.getInstance();
        logger = MessageBox.getLogger();
        Configuration prop = FloatSphere.createProperties();
        boolean enableReplaceSys = false;
        if(prop!=null){
            Object limit = prop.getValueByKey(SectionName.MAX_PLUGIN_LIMIT);
            if(limit!=null)
                MAX_PLUGIN = Integer.parseInt(String.valueOf(limit));
            Object replaceOut = prop.getValueByKey(SectionName.ENABLE_SYSTEM_OUT);
            if(replaceOut!=null){
                enableReplaceSys = Boolean.parseBoolean(String.valueOf(replaceOut));
            }
        }
        if(enableReplaceSys)
            System.setErr(new ExceptionPrinter(System.out));
    }
    @Override
    public void registerEventListener(Plugin plugin, Listener listener) {
        if(!plugin.isEnable()){
            MessageBox.getLogger().warn(plugin.getName()+"had disable!");
            return;
        }
        Map<Class<? extends Event>,Set<RegisteredListener>> map = processor.createRegistered(plugin,listener);
        for(Entry<Class<? extends Event>,Set<RegisteredListener>> entry : map.entrySet()){
            Class<? extends Event> eventClazz = entry.getKey();
            if(eventClazz!=null){
                HandlerList handlerList = getHandlerListByClazz(eventClazz);
                handlerList.registerAll(entry.getValue());
            }
        }
    }

    /**
     * 注销掉所有此插件注册过的监听器
     * @param plugin 删除的插件
     */
    public void unregisterEventListener(Plugin plugin){
        if(plugin.isEnable())
            return;
        HandlerList.unregisterAll(plugin);
    }

    /**
     * 从Class中获得静态变量HandlerList
     * @param type Class对象
     * @return 返回HandlerList或者Null
     */
    HandlerList getHandlerListByClazz(Class<? extends Event> type){
        try {
            Method method = searchClassAll(type).getDeclaredMethod("getHandlerList");
            method.setAccessible(true);
            return (HandlerList) method.invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 递归查找HandlerList方法
     * @param clazz 当前查找的Class对象
     * @return 返回含有这个方法的Class对象
     */
    private Class<? extends Event> searchClassAll(Class<? extends Event> clazz){
        try{
            clazz.getDeclaredMethod("getHandlerList");
            return clazz;
        } catch (NoSuchMethodException e) {
            Class<?> tempClass = clazz.getSuperclass();
            if(!tempClass.equals(Event.class)&&Event.class.isAssignableFrom(tempClass)){
                return searchClassAll(tempClass.asSubclass(Event.class));
            }
            else{
                MessageBox.getLogger().warn("Can not find a static method named getHandlerList in "+clazz.getSimpleName());
                throw new IllegalArgumentException();
            }
        }
    }
    @Override
    public void callEvent(Event event) {
        if(event==null)
            MessageBox.getLogger().warn("Event is not enable NULL!");
        if(event instanceof Cancelable)
        {
            if(((Cancelable) event).isCancel())
                return;
        }
        assert event != null;
        HandlerList handlerList = event.getHandler();
        if(handlerList==null){
            MessageBox.getLogger().warn("The Event can not search a static HandlerList ->"+event.getEventType());
            return;
        }
        fireEvent(event,handlerList.getListeners());
    }

    @Override
    public void enablePlugins() {
        if(libLoader==null){
            URL[] urls = new URL[1];
            try {
                URL url = new File(FloatSphere.getRootPath(),"libs").toURI().toURL();
                urls[0] = url;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            libLoader = new LibrariesClassLoader(urls,FloatPluginManager.class.getClassLoader());
            MessageBox.getLogger().warnTips("loaded libs:");
            for(String name : libLoader.getLibsName())
                MessageBox.getLogger().log(name);
        }
        long start = System.currentTimeMillis();
        int number = 0;
        number = loadPlugins();
        logger.log("Complete loads all plugins\nSuccess Loaded "+number+" Plugin Spend "+(System.currentTimeMillis()-start)+"ms");
    }

    protected int loadPlugins(){
        File file = new File(PLUGIN_PATH);
        File[] files;
        boolean flag = true;
        if(!file.exists())
            flag = file.mkdirs();
        files = file.listFiles(pathname -> pathname.getName().endsWith(".jar"));
        ArrayList<PluginEntry> pluginEntries = new ArrayList<>();
        if(files!=null&&flag){
            for (File value : files){
                try {
                    pluginEntries.add(FloatSphere.createPluginEntry(value));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            TopologyMachine topologyMachine = FloatSphere.createTopologyMachine();
            topologyMachine.sort(pluginEntries);
        }
        long  start;
        for(PluginEntry pluginEntry : pluginEntries){
            start = System.currentTimeMillis();
            try {
                if(processor.getPluginNumber()>=MAX_PLUGIN)
                    break;
                loadPlugin(pluginEntry);
            } catch (Exception | NoClassDefFoundError e) {
                MessageBox.getLogger().warn("["+pluginEntry.getPluginName()+"] crashed and your plugin could not be loaded!\n");
                e.printStackTrace();
            }
            MessageBox.getLogger().broadcastPlugin(pluginEntry.getPluginName(),start-pluginEntry.getStartTime());
        }
        return processor.getPluginNumber();
    }

    @Override
    public void disablePlugin(String pluginName) {
        processor.disablePlugin(((PluginProcessor)this.processor).getPlugin(pluginName));
        System.gc();
    }

    /**
     * 开放给用户的加载插件接口
     * @param pluginName 插件的名称
     * @throws Exception 报错
     */
    @Override
    public void loadPlugin(String pluginName) throws Exception {
        PluginEntry targetPlugin;
        File pluginFile = searchFileByName(pluginName,true);
        if(pluginFile==null){
            MessageBox.getLogger().warn("The plugin "+pluginName+" is not a plugin or can not find!");
            return;
        }
        targetPlugin = FloatSphere.createPluginEntry(pluginFile);
        try{
            this.processor.loadPlugin(targetPlugin,libLoader);
        }
        catch (NullPointerException e){
            assert targetPlugin != null;
            MessageBox.getLogger().warn("We can not load this plugin ->" + targetPlugin.getPluginName());
        }
    }

    @Override
    public List<String> getPluginList() {
        List<String> strList = null;
        if(processor!=null)
            strList = processor.getPluginList();
        return strList;
    }

    @Override
    public void noticeDis(String name) {
        disablePlugins.add(name);
    }

    /**
     * 从plugins目录下找插件
     * @param fileName 插件的文件名称或独一名称
     * @return 返回插件文件或对象
     */
    public File searchFileByName(String fileName,boolean checkInner){
        String targetName = fileName+".jar";
        File dataFolder = new File(PLUGIN_PATH);
        File ans = null;
        if(dataFolder.exists()){
            if(dataFolder.isDirectory()){
                File[] files = dataFolder.listFiles();
                if(files!=null){
                    int i = 0;
                    while(i<files.length){
                        File tempFile = null;
                        do{
                            if(i>=files.length)
                                break;
                            tempFile = files[i++];
                        }while(!tempFile.getName().equals(targetName));
                        if(tempFile.getName().equals(targetName)){
                            ans = tempFile;
                            break;
                        }
                    }
                    if(ans==null&&checkInner){
                        File tempFile = null;
                        i = 0;
                        while(i<files.length){
                            do{
                                if(i>=files.length)
                                    break;
                                tempFile = files[i++];
                            }while(!tempFile.getName().endsWith(".jar"));
                            if(!tempFile.getName().endsWith(".jar"))
                                break;
                            Configuration pluginYml = ConfigurationUtil.getPluginConfigYML(tempFile);
                            if(pluginYml!=null){
                                String name = pluginYml.getString("name");
                                if(name.concat(".jar").equals(targetName)){
                                    ans = tempFile;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return ans;
    }

    /**
     * 系统内部调用的加载插件接口
     * @param entry 插件对象
     * @throws Exception 报错
     */
    private void loadPlugin(PluginEntry entry) throws Exception {
        processor.loadPlugin(entry,libLoader);
    }
    private void fireEvent(Event event,RegisteredListener[] registeredListeners){
        for(RegisteredListener listener : registeredListeners){
            Plugin plugin = listener.getPlugin();
            if(plugin.isEnable()){
                if(((Cancelable)event).isCancel())
                    break;
                listener.callEvent(event);
            }
        }
    }
}
