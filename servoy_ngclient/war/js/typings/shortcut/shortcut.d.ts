interface Shortcut {
    all_shortcuts: any;
    add(shortcut_combination: string, callback: any): void;
    add(shortcut_combination: string, callback: any, opt: any): void;
    remove(shortcut_combination: string);
}