package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.module.Module;
import myau.util.ChatUtil;
import myau.util.KeyBindUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BindCommand extends Command {
    public BindCommand() {
        super(new ArrayList<>(Arrays.asList("bind", "b")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 3) {
            if (args.size() == 2 && (args.get(1).equalsIgnoreCase("l") || args.get(1).equalsIgnoreCase("list"))) {
                List<Module> modules = Myau.moduleManager.modules.values().stream().filter(module -> module.getKey() != 0).collect(Collectors.toList());
                if (modules.isEmpty()) {
                    ChatUtil.sendFormatted(String.format("%sNo binds&r", Myau.clientName));
                } else {
                    ChatUtil.sendFormatted(String.format("%sBinds:&r", Myau.clientName));
                    for (Module module : modules) {
                        ChatUtil.sendFormatted(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
                    }
                }
            } else {
                ChatUtil.sendFormatted(
                        String.format(
                                "%sUsage: .%s <&omodule&r> <&okey&r>&r | .%s <&omodule&r> &onone&r | .%s &olist&r",
                                Myau.clientName,
                                args.get(0).toLowerCase(Locale.ROOT),
                                args.get(0).toLowerCase(Locale.ROOT),
                                args.get(0).toLowerCase(Locale.ROOT)
                        )
                );
            }
        } else {
            String keyInput = args.get(2).toUpperCase();
            int keyIndex = 0;

            if (keyInput.equalsIgnoreCase("NONE") || keyInput.equalsIgnoreCase("NULL") || keyInput.equalsIgnoreCase("0")) {
                keyIndex = 0;
            } else {
                keyIndex = Keyboard.getKeyIndex(keyInput);

                if (keyIndex == 0) {
                    int buttonIndex = getMouseButtonIndex(keyInput);
                    if (buttonIndex != -1) {
                        keyIndex = buttonIndex - 100;
                    }
                }
            }

            if (!args.get(1).equals("*")) {
                Module module = Myau.moduleManager.getModule(args.get(1));
                if (module == null) {
                    ChatUtil.sendFormatted(String.format("%sModule not found (&o%s&r)&r", Myau.clientName, args.get(1)));
                } else {
                    module.setKey(keyIndex);
                    if (keyIndex == 0) {
                        ChatUtil.sendFormatted(
                                String.format("%sUnbind &o%s&r", Myau.clientName, module.getName())
                        );
                    } else {
                        ChatUtil.sendFormatted(
                                String.format("%sBound &o%s&r to &l[%s]&r", Myau.clientName, module.getName(), KeyBindUtil.getKeyName(keyIndex))
                        );
                    }
                }
            } else {
                for (Module module : Myau.moduleManager.modules.values()) {
                    module.setKey(keyIndex);
                }
                if (keyIndex == 0) {
                    ChatUtil.sendFormatted(
                            String.format("%sUnbind all modules&r", Myau.clientName)
                    );
                } else {
                    ChatUtil.sendFormatted(
                            String.format("%sBind all modules to &l[%s]&r", Myau.clientName, KeyBindUtil.getKeyName(keyIndex))
                    );
                }
            }
        }
    }

    private int getMouseButtonIndex(String buttonName) {
        // Handle numbered format (MOUSE0, MOUSE1, etc.)
        if (buttonName.startsWith("MOUSE")) {
            try {
                String numStr = buttonName.substring(5);
                int buttonNum = Integer.parseInt(numStr);
                if (buttonNum >= 0 && buttonNum < Mouse.getButtonCount()) {
                    return buttonNum;
                }
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            }
        }

        int buttonIndex = Mouse.getButtonIndex(buttonName);
        if (buttonIndex != -1) {
            return buttonIndex;
        }

        switch (buttonName) {
            case "LBUTTON":
            case "LMB":
            case "LEFTCLICK":
                return 0;
            case "RBUTTON":
            case "RMB":
            case "RIGHTCLICK":
                return 1;
            case "MBUTTON":
            case "MMB":
            case "MIDDLECLICK":
            case "SCROLLCLICK":
                return 2;
            case "MOUSE3":
            case "XBUTTON1":
            case "SIDEBUTTON1":
            case "BOTTOMSIDE":
                return 3;
            case "MOUSE4":
            case "XBUTTON2":
            case "SIDEBUTTON2":
            case "TOPSIDE":
                return 4;
            case "MOUSE5":
                return 5;
            case "MOUSE6":
                return 6;
            case "MOUSE7":
                return 7;
            default:
                return -1;
        }
    }
}
