package com.chbcraft.internals.components.sysevent;

import com.chbcraft.internals.components.listen.HandlerList;

public abstract class Event {
    /**
     * 存储事件的类别
     */
    private String eventType;

    /**
     * 事件类型的名称
     * @return 返回事件的类型，例如PluginLoadEvent
     */
    public String getEventType(){
        if(eventType==null){
            eventType = this.getClass().getSimpleName();
        }
        return eventType;
    }

    /**
     * 获得这个时间对应的监听器集合对象
     * @return 返回集合对象
     */
    public abstract HandlerList getHandler();
    public static enum EventState{
        ENABLE,
        CANCELABLE;

    }
}
