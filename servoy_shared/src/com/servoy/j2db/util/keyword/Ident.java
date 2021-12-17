/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.j2db.util.keyword;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.servoy.j2db.util.Utils;


/**
 * @author jblok
 */
@SuppressWarnings("nls")
public class Ident
{
	private final static Set<String> java_js_common_keywords = new HashSet<>(Arrays.asList(
		// Keywords disallowed both in java and javascript
		"null", //
		"export", //
		"enum", //
		"native", //
		"abstract", //
		"double", //
		"int", //
		"boolean", //
		"interface", //
		"super", //
		"extends", //
		"long", //
		"byte", //
		"final", //
		"synchronized", //
		"float", //
		"package", //
		"char", //
		"private", //
		"throws", //
		"class", //
		"goto", //
		"protected", //
		"transient", //
		"const", //
		"public", //
		"implements", //
		"import", //
		"short", //
		"volatile", //
		"static" //
	));


	private final static Set<String> java_keywords = new HashSet<>(java_js_common_keywords);
	static
	{
		// Keywords allowed in javascript but not in java
		java_keywords.addAll(Arrays.asList(
			"strictfp", //
			"else", //
			"break", //
			"switch", //
			"case", //
			"finally", //
			"new", //
			"this", //
			"catch", //
			"throw", //
			"for", //
			"if", //
			"try", //
			"continue", //
			"return", //
			"void", //
			"default", //
			"do", //
			"instanceof", //
			"while" //
		));
	}

	private final static Set<String> js_keywords = new HashSet<>(java_js_common_keywords);
	static
	{
		// Keywords allowed in java but not in javascript
		js_keywords.addAll(Arrays.asList(
			"undefined", //
			"constant", //
			"function", //
			"debugger", //
			"in", //
			"typeof", //
			"export", //
			"with", //
			"delete", //
			"date", // to prevent Date
			"array", // to prevent Array
			"arguments" // to prevent an dataprovider with the name arguments
		));
	}

	private final static Set<String> servoy_keywords = new HashSet<>(Arrays.asList(
		//Standard j2db DOM things
		"databaseManager", //
		"datasources", //
		"application", //
		"currentform", //
		"currentcontroller", //
		"currentRecordIndex", //
		"history", //
		"math", //
		"form", //
		"controller", //
		"elements", //
		"length", //
		"globals", //
		"scopes", //
		"plugins", //
		"forms", //
		"foundset", //
		"utils", //
		"security", //
		"solutionModel", //
		"recordIndex", //
		"allnames", //
		"allmethods", //
		"allrelations", //
		"allvariables", //
		"exception", //
		"jsunit", //
		"servoyDeveloper", //
		// New
		"_super"));

	private final static Set<String> reserved_os_words = new HashSet<>(Arrays.asList(
		// Words that cannot be used on all OS platforms
		"aux", //
		"com1", //
		"com2", //
		"com3", //
		"com4", //
		"com5", //
		"com6", //
		"com7", //
		"com8", //
		"com9", //
		"con", //
		"lpt1", //
		"lpt2", //
		"lpt3", //
		"lpt4", //
		"lpt5", //
		"lpt6", //
		"lpt7", //
		"lpt8", //
		"lpt9", //
		"nul", //
		"prn" //
	));

	private static final Set<String> mobile_window_reserved_words = new HashSet<String>(Arrays.asList("addEventListener", "alert", "applicationCache",
		"ArrayBuffer", "atob", "Attr", "Audio", "AudioProcessingEvent", "back", "BeforeLoadEvent", "Blob", "blur", "btoa", "CanvasGradient", "CanvasPattern",
		"CanvasRenderingContext2D", "captureEvents", "CDATASection", "CharacterData", "chrome", "clearInterval", "clearTimeout", "clientInformation",
		"ClientRect", "ClientRectList", "Clipboard", "close", "closed", "CloseEvent", "Comment", "CompositionEvent", "confirm", "console", "content",
		"controllers", "Counter", "createPopup", "crypto", "CSSCharsetRule", "CSSFontFaceRule", "CSSImportRule", "CSSMediaRule", "CSSPageRule",
		"CSSPrimitiveValue", "CSSRule", "CSSRuleList", "CSSStyleDeclaration", "CSSStyleRule", "CSSStyleSheet", "CSSValue", "CSSValueList", "CustomEvent",
		"DataView", "defaultstatus", "defaultStatus", "DeviceOrientationEvent", "devicePixelRatio", "disableExternalCapture", "dispatchEvent", "document",
		"Document", "DocumentFragment", "DocumentType", "DOMException", "DOMImplementation", "DOMParser", "DOMSettableTokenList", "DOMStringList",
		"DOMStringMap", "DOMTokenList", "dump", "Element", "enableExternalCapture", "Entity", "EntityReference", "ErrorEvent", "event", "Event",
		"EventException", "EventSource", "external", "File", "FileError", "FileList", "FileReader", "find", "Float32Array", "Float64Array", "focus",
		"FormData", "forward", "frameElement", "frames", "fullScreen", "getComputedStyle", "getMatchedCSSRules", "getSelection", "HashChangeEvent", "history",
		"home", "HTMLAllCollection", "HTMLAnchorElement", "HTMLAppletElement", "HTMLAreaElement", "HTMLAudioElement", "HTMLBaseElement", "HTMLBaseFontElement",
		"HTMLBodyElement", "HTMLBRElement", "HTMLButtonElement", "HTMLCanvasElement", "HTMLCollection", "HTMLDirectoryElement", "HTMLDivElement",
		"HTMLDListElement", "HTMLDocument", "HTMLElement", "HTMLEmbedElement", "HTMLFieldSetElement", "HTMLFontElement", "HTMLFormElement", "HTMLFrameElement",
		"HTMLFrameSetElement", "HTMLHeadElement", "HTMLHeadingElement", "HTMLHRElement", "HTMLHtmlElement", "HTMLIFrameElement", "HTMLImageElement",
		"HTMLInputElement", "HTMLKeygenElement", "HTMLLabelElement", "HTMLLegendElement", "HTMLLIElement", "HTMLLinkElement", "HTMLMapElement",
		"HTMLMarqueeElement", "HTMLMediaElement", "HTMLMenuElement", "HTMLMetaElement", "HTMLMeterElement", "HTMLModElement", "HTMLObjectElement",
		"HTMLOListElement", "HTMLOptGroupElement", "HTMLOptionElement", "HTMLOutputElement", "HTMLParagraphElement", "HTMLParamElement", "HTMLPreElement",
		"HTMLProgressElement", "HTMLQuoteElement", "HTMLScriptElement", "HTMLSelectElement", "HTMLSourceElement", "HTMLSpanElement", "HTMLStyleElement",
		"HTMLTableCaptionElement", "HTMLTableCellElement", "HTMLTableColElement", "HTMLTableElement", "HTMLTableRowElement", "HTMLTableSectionElement",
		"HTMLTextAreaElement", "HTMLTitleElement", "HTMLUListElement", "HTMLUnknownElement", "HTMLVideoElement", "IceCandidate", "Image", "ImageData",
		"indexedDB", "innerHeight", "innerWidth", "InstallTrigger", "Int16Array", "Int32Array", "Int8Array", "KeyboardEvent", "length", "localStorage",
		"location", "locationbar", "matchMedia", "MediaController", "MediaError", "MediaList", "MediaStreamEvent", "menubar", "MessageChannel", "MessageEvent",
		"MessagePort", "MimeType", "MimeTypeArray", "MouseEvent", "moveBy", "moveTo", "mozAnimationStartTime", "mozCancelAnimationFrame",
		"mozCancelRequestAnimationFrame", "mozIndexedDB", "mozInnerScreenX", "mozInnerScreenY", "mozPaintCount", "mozRequestAnimationFrame", "MutationEvent",
		"name", "NamedNodeMap", "navigator", "Node", "NodeFilter", "NodeList", "Notation", "OfflineAudioCompletionEvent", "offscreenBuffering", "onabort",
		"onafterprint", "onafterscriptexecute", "onbeforeprint", "onbeforescriptexecute", "onbeforeunload", "onblur", "oncanplay", "oncanplaythrough",
		"onchange", "onclick", "oncontextmenu", "oncopy", "oncut", "ondblclick", "ondevicelight", "ondevicemotion", "ondeviceorientation", "ondeviceproximity",
		"ondrag", "ondragend", "ondragenter", "ondragleave", "ondragover", "ondragstart", "ondrop", "ondurationchange", "onemptied", "onended", "onerror",
		"onfocus", "onhashchange", "oninput", "oninvalid", "onkeydown", "onkeypress", "onkeyup", "onload", "onloadeddata", "onloadedmetadata", "onloadstart",
		"onmessage", "onmousedown", "onmouseenter", "onmouseleave", "onmousemove", "onmouseout", "onmouseover", "onmouseup", "onmousewheel",
		"onmozfullscreenchange", "onmozfullscreenerror", "onmozpointerlockchange", "onmozpointerlockerror", "onoffline", "ononline", "onpagehide",
		"onpageshow", "onpaste", "onpause", "onplay", "onplaying", "onpopstate", "onprogress", "onratechange", "onreset", "onresize", "onscroll", "onsearch",
		"onseeked", "onseeking", "onselect", "onshow", "onstalled", "onstorage", "onsubmit", "onsuspend", "ontimeupdate", "onunload", "onuserproximity",
		"onvolumechange", "onwaiting", "onwebkitanimationend", "onwebkitanimationiteration", "onwebkitanimationstart", "onwebkittransitionend", "onwheel",
		"open", "openDatabase", "openDialog", "opener", "Option", "outerHeight", "outerWidth", "OverflowEvent", "PageTransitionEvent", "pageXOffset",
		"pageYOffset", "parent", "performance", "PERSISTENT", "personalbar", "pkcs11", "Plugin", "PluginArray", "PopStateEvent", "postMessage", "print",
		"ProcessingInstruction", "ProgressEvent", "prompt", "Range", "RangeException", "Rect", "releaseEvents", "removeEventListener", "resizeBy", "resizeTo",
		"RGBColor", "routeEvent", "screen", "screenLeft", "screenTop", "screenX", "screenY", "scroll", "scrollbars", "scrollBy", "scrollByLines",
		"scrollByPages", "scrollMaxX", "scrollMaxY", "scrollTo", "scrollX", "scrollY", "Selection", "self", "SessionDescription", "sessionStorage",
		"setInterval", "setResizable", "setTimeout", "SharedWorker", "showModalDialog", "sizeToContent", "SpeechInputEvent", "SQLException", "status",
		"statusbar", "stop", "Storage", "StorageEvent", "styleMedia", "StyleSheet", "StyleSheetList", "SVGAElement", "SVGAltGlyphDefElement",
		"SVGAltGlyphElement", "SVGAltGlyphItemElement", "SVGAngle", "SVGAnimateColorElement", "SVGAnimatedAngle", "SVGAnimatedBoolean",
		"SVGAnimatedEnumeration", "SVGAnimatedInteger", "SVGAnimatedLength", "SVGAnimatedLengthList", "SVGAnimatedNumber", "SVGAnimatedNumberList",
		"SVGAnimatedPreserveAspectRatio", "SVGAnimatedRect", "SVGAnimatedString", "SVGAnimatedTransformList", "SVGAnimateElement", "SVGAnimateMotionElement",
		"SVGAnimateTransformElement", "SVGCircleElement", "SVGClipPathElement", "SVGColor", "SVGComponentTransferFunctionElement", "SVGCursorElement",
		"SVGDefsElement", "SVGDescElement", "SVGDocument", "SVGElement", "SVGElementInstance", "SVGElementInstanceList", "SVGEllipseElement", "SVGException",
		"SVGFEBlendElement", "SVGFEColorMatrixElement", "SVGFEComponentTransferElement", "SVGFECompositeElement", "SVGFEConvolveMatrixElement",
		"SVGFEDiffuseLightingElement", "SVGFEDisplacementMapElement", "SVGFEDistantLightElement", "SVGFEDropShadowElement", "SVGFEFloodElement",
		"SVGFEFuncAElement", "SVGFEFuncBElement", "SVGFEFuncGElement", "SVGFEFuncRElement", "SVGFEGaussianBlurElement", "SVGFEImageElement",
		"SVGFEMergeElement", "SVGFEMergeNodeElement", "SVGFEMorphologyElement", "SVGFEOffsetElement", "SVGFEPointLightElement", "SVGFESpecularLightingElement",
		"SVGFESpotLightElement", "SVGFETileElement", "SVGFETurbulenceElement", "SVGFilterElement", "SVGFontElement", "SVGFontFaceElement",
		"SVGFontFaceFormatElement", "SVGFontFaceNameElement", "SVGFontFaceSrcElement", "SVGFontFaceUriElement", "SVGForeignObjectElement", "SVGGElement",
		"SVGGlyphElement", "SVGGlyphRefElement", "SVGGradientElement", "SVGHKernElement", "SVGImageElement", "SVGLength", "SVGLengthList",
		"SVGLinearGradientElement", "SVGLineElement", "SVGMarkerElement", "SVGMaskElement", "SVGMatrix", "SVGMetadataElement", "SVGMissingGlyphElement",
		"SVGMPathElement", "SVGNumber", "SVGNumberList", "SVGPaint", "SVGPathElement", "SVGPathSeg", "SVGPathSegArcAbs", "SVGPathSegArcRel",
		"SVGPathSegClosePath", "SVGPathSegCurvetoCubicAbs", "SVGPathSegCurvetoCubicRel", "SVGPathSegCurvetoCubicSmoothAbs", "SVGPathSegCurvetoCubicSmoothRel",
		"SVGPathSegCurvetoQuadraticAbs", "SVGPathSegCurvetoQuadraticRel", "SVGPathSegCurvetoQuadraticSmoothAbs", "SVGPathSegCurvetoQuadraticSmoothRel",
		"SVGPathSegLinetoAbs", "SVGPathSegLinetoHorizontalAbs", "SVGPathSegLinetoHorizontalRel", "SVGPathSegLinetoRel", "SVGPathSegLinetoVerticalAbs",
		"SVGPathSegLinetoVerticalRel", "SVGPathSegList", "SVGPathSegMovetoAbs", "SVGPathSegMovetoRel", "SVGPatternElement", "SVGPoint", "SVGPointList",
		"SVGPolygonElement", "SVGPolylineElement", "SVGPreserveAspectRatio", "SVGRadialGradientElement", "SVGRect", "SVGRectElement", "SVGRenderingIntent",
		"SVGScriptElement", "SVGSetElement", "SVGStopElement", "SVGStringList", "SVGStyleElement", "SVGSVGElement", "SVGSwitchElement", "SVGSymbolElement",
		"SVGTextContentElement", "SVGTextElement", "SVGTextPathElement", "SVGTextPositioningElement", "SVGTitleElement", "SVGTransform", "SVGTransformList",
		"SVGTRefElement", "SVGTSpanElement", "SVGUnitTypes", "SVGUseElement", "SVGViewElement", "SVGVKernElement", "SVGZoomEvent", "TEMPORARY", "Text",
		"TextEvent", "TextMetrics", "TimeRanges", "toolbar", "top", "TouchEvent", "UIEvent", "Uint16Array", "Uint32Array", "Uint8Array", "Uint8ClampedArray",
		"updateCommands", "URL", "v8Locale", "WebGLActiveInfo", "WebGLBuffer", "WebGLContextEvent", "WebGLFramebuffer", "WebGLProgram", "WebGLRenderbuffer",
		"WebGLRenderingContext", "WebGLShader", "WebGLTexture", "WebGLUniformLocation", "WebKitAnimationEvent", "webkitAudioContext", "webkitAudioPannerNode",
		"WebKitBlobBuilder", "webkitCancelAnimationFrame", "webkitCancelRequestAnimationFrame", "webkitConvertPointFromNodeToPage",
		"webkitConvertPointFromPageToNode", "WebKitCSSFilterValue", "WebKitCSSKeyframeRule", "WebKitCSSKeyframesRule", "WebKitCSSMatrix",
		"WebKitCSSRegionRule", "WebKitCSSTransformValue", "webkitIDBCursor", "webkitIDBDatabase", "webkitIDBDatabaseError", "webkitIDBDatabaseException",
		"webkitIDBFactory", "webkitIDBIndex", "webkitIDBKeyRange", "webkitIDBObjectStore", "webkitIDBRequest", "webkitIDBTransaction", "webkitIndexedDB",
		"WebKitIntent", "WebKitMutationObserver", "webkitNotifications", "WebKitPoint", "webkitPostMessage", "webkitRequestAnimationFrame",
		"webkitRequestFileSystem", "webkitResolveLocalFileSystemURL", "webkitStorageInfo", "WebKitTransitionEvent", "webkitURL", "WebSocket", "WheelEvent",
		"window", "Window", "Worker", "XMLDocument", "XMLHttpRequest", "XMLHttpRequestException", "XMLHttpRequestProgressEvent", "XMLHttpRequestUpload",
		"XMLSerializer", "XPathEvaluator", "XPathException", "XPathResult", "XSLTProcessor"));

	private static final Set<String> mobile_window_gwt_used_words = new HashSet<String>(Arrays.asList("i"));

	public static boolean checkIfKeyword(String name)
	{
		return checkName(java_keywords, name) || checkIfScriptingKeyword(name);
	}

	public static boolean checkIfScriptingKeyword(String name)
	{
		return checkName(js_keywords, name) || checkName(servoy_keywords, name);
	}

	public static boolean checkIfReservedOSWord(String name)
	{
		return checkName(reserved_os_words, name);
	}

	public static boolean checkIfReservedBrowserWindowObjectWord(String name)
	{
		// case sensitive search
		return name != null && (mobile_window_reserved_words.contains(name.trim()) || mobile_window_gwt_used_words.contains(name.trim()));
	}

	private static boolean checkName(Set<String> names, String name)
	{
		if (name == null) return false;
		String lname = name.trim().toLowerCase();
		return names.contains(lname);
	}

	public static String generateNormalizedName(String plainSQLName)
	{
		if (plainSQLName == null) return null;

		String name = Utils.toEnglishLocaleLowerCase(plainSQLName.trim());//to lower case because the not all databases support camelcasing and jdbc drivers comeback with all to upper or lower

		char[] chars = name.toCharArray();
		boolean replaced = false;
		for (int i = 0; i < chars.length; i++)
		{
			switch (chars[i])
			{
				// not allowed in windows
				case '/' :
				case '\\' :
				case '?' :
				case '%' :
				case '*' :
				case ':' :
				case '|' :
				case '"' :
				case '<' :
				case '>' :
					// not allowed in scripting
				case ' ' :
				case '-' :
				case '.' :
					chars[i] = '_';
					replaced = true;
					break;
			}
		}

		return replaced ? new String(chars) : name;
	}

	public static final String RESERVED_NAME_PREFIX = "_"; //$NON-NLS-1$

	public static String generateNormalizedNonKeywordName(String plainSQLName)
	{
		String name = generateNormalizedName(plainSQLName);
		if (checkIfKeyword(name))
		{
			name = RESERVED_NAME_PREFIX + name;
		}
		return name;
	}

	public static String generateNormalizedNonReservedOSName(String plainSQLName)
	{
		String name = generateNormalizedName(plainSQLName);
		if (checkIfReservedOSWord(name))
		{
			name = RESERVED_NAME_PREFIX + name;
		}
		return name;
	}

	public static Set<String> getJavaJsKeywords()
	{
		Set<String> javaJsKeywords = new HashSet<>(java_keywords);
		javaJsKeywords.addAll(js_keywords);
		return javaJsKeywords;
	}

	public static Set<String> getServoyKeywords()
	{
		return servoy_keywords;
	}
}
