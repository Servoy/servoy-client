/// <reference path="../angularjs/angular.d.ts" />

declare namespace servoy {
	interface IRootScopeService extends angular.IRootScopeService {
		updatingFormName: string;
		updatingFormUrl:string;		
	}
	
	interface SolutionSettings {
		solutionName:string,
		windowName:string,	
		mainForm: {name:string},
		navigatorForm: {name:string,size:{width:number}},
		solutionTitle: string,
		styleSheetPaths: string[],
		ltrOrientation : boolean,
		enableAnchoring: boolean,
		noLicense:{viewUrl:string,redirectUrl:string,redirectTimeout:number},
		maintenanceMode:{viewUrl:string,redirectUrl:string,redirectTimeout:number},
		sessionExpired:{viewUrl:string,redirectUrl:string},
		internalServerError:{viewUrl:string},		
	}
	
	interface WindowType{
		DIALOG:number,
		MODAL_DIALOG:number,
		WINDOW:number,
	}
	
	interface FormType{
		name:string,
		size:{width:number,height:number}
	}

	interface WindowInstance {
		name:string,
		type:number, // should be enum
		title:string,
		navigatorForm:FormType,
		form:FormType,
		size:{width:number,height:number},
		location:{x:number,y:number},
		initialBounds:{x:number,y:number,width:number,height:number}
		storeBounds:boolean,
		opacity:number,
		undecorated:boolean,
		cssClassName:string,
		resizable:boolean,
		transparent:boolean,
		bsWindowInstance:any,  // bootstrap-window instance , available only after creation
		$scope:angular.IScope,
		hide():void,
		setLocation(location:{x:number,y:number}):void,
		setSize(size:{width:number,height:number}):void,
		getSize():{width:number,height:number},
		onResize($event,size:{width:number,height:number}): void,
		onMove($event,location:{x:number,y:number}):void,
		toFront():void,
		toBack():void,
		clearBounds():void
	}
	
	
	interface IWindowService {
	    getLoadedFormUrls(): { [s: string]: string; },
		create(name:string,type:number):WindowInstance,
		show(name:string,form:string, title:string): void,
		hide(name:string):void,
		destroy(name):void,
		switchForm(name:string,form:FormType,navigatorForm:FormType):void,
		setTitle(name:string,title:string):void,
		setInitialBounds(name:string,initialBounds:{x:number,y:number,width:number,height:number}): void,
		setStoreBounds(name:string,storeBounds:boolean):void,
		resetBounds(name:string):void,
		setLocation(name:string,location:{x:number,y:number}):void,
		setSize(name:string,size:{width:number,height:number}):void,
		getSize(name):{width:number,height:number},
		setUndecorated(name:string,undecorated:boolean):void,
		setCSSClassName(name:string,cssClassName:string):void,
		setOpacity(name:string,opacity:number):void,
		setResizable(name:string,resizable:boolean):void,
		setTransparent(name:string,transparent:boolean):void,
		toFront(name:string):void,
		toBack(name:string):void,
		reload():void,
		updateController(formName:string,controllerCode:string, realFormUrl:string, forceLoad:boolean, html:string):void,
		requireFormLoaded(formName:string):void,
		destroyController(formName:string):void,
		getFormUrl(formName:string):string
	}
	
	interface IServoyWindowManager {
		BSWindowManager: any,
		instances: { [s: string]: WindowInstance; },
		open(windowOptions:{windowInstance:servoy.WindowInstance,resolve?:{},templateUrl?:string,template?:string,controller:string,scope?:angular.IScope}): angular.IPromise<boolean>,
		destroyAllDialogs(): void,
	}
	
	interface IMainControllerScope extends angular.IScope {
		solutionSettings: SolutionSettings,
		getMainFormUrl():string,
		getNavigatorFormUrl():string,
		getSessionProblemView():string,
		getNavigatorStyle(ltrOrientation:string):any,
		getFormStyle(ltrOrientation):any
	}
	
	interface IServoyI18NService {
		addDefaultTranslations(translations):void,
		getI18NMessages():angular.IPromise<{}>, // this uses the arguments as a variable arguments array..
		getI18NMessage(key:string):string|angular.IPromise<{}>,
		flush():void,
	}
	
	interface IServoyProperties {
		setBorder(element:JQuery,newVal):void,
		setCssProperty(element:JQuery, cssPropertyName:string,newVal):void,
		setHorizontalAlignment(element:JQuery,halign:number):void,
		setHorizontalAlignmentFlexbox(element:JQuery,halign:number):void,
		setVerticalAlignment(element:JQuery,valign:number):void,
		setRotation(element:JQuery,scope:angular.IScope&{model:{size:{height:number,width:number}}},rotation:number):void,
		addSelectOnEnter(element:JQuery):void,
		getScrollbarsStyleObj(scrollbars:number):{overflowX?:string,overflowY?:string},
		setScrollbars(element:JQuery, value:number):void,
		createTooltipState(element:JQuery,value:any):void,
	}
	
	interface IUtils {
		notNullOrEmpty(propPath:string):(item:any)=>boolean,
		autoApplyStyle(scope:angular.IScope,element:JQuery,modelToWatch,cssPropertyName:string):void,
		getEventHandler($parse:angular.IParseService,scope:angular.IScope,svyEventHandler:string):angular.ICompiledExpression,
		attachEventHandler($parse:angular.IParseService,element:JQuery,scope:angular.IScope,svyEventHandler,domEvent:string, filterFunction?,timeout?:number,returnFalse?:boolean, doSvyApply?:boolean, dataproviderString?:string, preHandlerCallback?):void,
		testEnterKey(e):boolean, 
		bindTwoWayObjectProperty(a, propertyNameA:string, b, propertyNameB:string, useObjectEquality:boolean, scope:angular.IScope):[()=>void],
		findAttribute(element:JQuery, parent:angular.IScope, attributeName:string),
	}
} 