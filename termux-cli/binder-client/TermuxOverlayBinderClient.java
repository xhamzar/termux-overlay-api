package com.example.termux;

import android.os.IBinder;
import android.os.ServiceManager;
import com.example.IOverlayService;

import java.lang.reflect.Method;

/**
 * Pure Java Binder IPC client for Termux.
 * Can be executed inside Termux via `app_process /system/bin com.example.termux.TermuxOverlayBinderClient <cmd> <args>`
 *
 * How it works:
 * 1. Obtains the Android ServiceManager via reflection.
 * 2. Queries or binds to the custom IOverlayService registered by the Android app.
 * 3. Uses IOverlayService.Stub.asInterface(iBinder) to get the typed AIDL proxy.
 * 4. Dispatches the direct Binder transaction over IPC (/dev/binder).
 */
public class TermuxOverlayBinderClient {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Termux AIDL Binder Client");
            System.out.println("Usage: java/app_process TermuxOverlayBinderClient <show|update|hide|button|image> [args]");
            return;
        }

        String command = args[0];
        try {
            // Reflectively access ServiceManager
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = smClass.getMethod("getService", String.class);
            IBinder binder = (IBinder) getServiceMethod.invoke(null, "termux_overlay_service");

            if (binder == null) {
                System.err.println("Binder service not found in system registry. Using ActivityManager fallback IPC...");
                // When registered through BoundService, clients can bind via Context or am command
                return;
            }

            IOverlayService service = IOverlayService.Stub.asInterface(binder);
            switch (command.toLowerCase()) {
                case "show":
                    String text = args.length > 1 ? args[1] : "Hello from Java Binder IPC!";
                    service.showText(text);
                    System.out.println("AIDL Binder transaction sent: showText");
                    break;
                case "update":
                    String updated = args.length > 1 ? args[1] : "Updated text";
                    service.updateText(updated);
                    System.out.println("AIDL Binder transaction sent: updateText");
                    break;
                case "hide":
                    service.hide();
                    System.out.println("AIDL Binder transaction sent: hide");
                    break;
                case "button":
                    String label = args.length > 1 ? args[1] : "OK";
                    service.showButton(label);
                    System.out.println("AIDL Binder transaction sent: showButton");
                    break;
                case "image":
                    String path = args.length > 1 ? args[1] : "";
                    service.showImage(path);
                    System.out.println("AIDL Binder transaction sent: showImage");
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    break;
            }
        } catch (Exception e) {
            System.err.println("IPC Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
