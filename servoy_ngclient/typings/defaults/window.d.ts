/// <reference path="../shortcut/shortcut.d.ts" />

interface Window {
    WindowManager: any;
    shortcut: Shortcut;
    servoy_remoteaddr: any;
    servoy_remotehost: any;
	servoy_remoteUTCOffset: any;
    msRequestAnimationFrame(...args: any[]): any;
    mozRequestAnimationFrame(...args: any[]): any;
    webkitRequestAnimationFrame(...args: any[]): any;
}