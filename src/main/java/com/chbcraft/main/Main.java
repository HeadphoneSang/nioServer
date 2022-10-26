package com.chbcraft.main;
import com.chbcraft.internals.components.FloatSphere;
import com.chbcraft.internals.components.*;
import com.chbcraft.internals.components.sysevent.PluginCommandEvent;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        MessageBox log = MessageBox.getLogger();
        PluginManager manager = FloatSphere.getPluginManager();
        manager.enablePlugins();Scanner scanner = new Scanner(System.in);
        while(true){
            String command  = scanner.nextLine();
            manager.callEvent(new PluginCommandEvent(command));
        }
    }
}

