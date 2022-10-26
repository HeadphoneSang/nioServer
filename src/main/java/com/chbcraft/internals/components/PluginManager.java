package com.chbcraft.internals.components;

import com.chbcraft.internals.components.sysevent.Event;
import com.chbcraft.plugin.Plugin;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public interface PluginManager {
    public void registerEventListener(Plugin plugin,Listener listener);
    public void unregisterEventListener(Plugin plugin);
    public void callEvent(Event event);
    public void enablePlugins();
    public void disablePlugin(String pluginName);
    public void loadPlugin(String pluginName) throws Exception;
    public List<String> getPluginList();
    public void noticeDis(String name);
}
