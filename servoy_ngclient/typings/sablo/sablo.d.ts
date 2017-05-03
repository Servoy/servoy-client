/// <reference path="../angularjs/angular.d.ts" />

declare namespace sablo { 
	interface ILogService extends angular.ILogService {
		debugEnabled: boolean;
		debugLevel: number;
		DEBUG: number;
		SPAM: number;
	}
	interface FormState {
		model:any;
		layout:any;
		properties: any;
		style:any;
		initializing:boolean;
		initialDataRequested: boolean;
		blockPostLinkInHidden: boolean;
		resolving: boolean;
		getScope(): angular.IScope;			
	}
	interface WSSession {
		callService<T>(serviceName:string, methodName:string, argsObject, async:boolean):angular.IPromise<T>;
		sendMessageObject:()=>void;
		onopen:(handler:(evt)=>void)=>void;
		onerror:()=>void;
		onclose:()=>void;
		onMessageObject(handler:(msg, conversionInfo, scopesToDigest)=>void): void; 
	}
	interface Locale {
		language:string;
		country:string;
		full: string;
	}
	interface SabloConstants {
		modelChangeNotifier:string	
	}
	interface ISabloApplication {
        connect(context, queryArgs, websocketUri): WSSession;
        contributeFormResolver(contributedFormResolver:{prepareUnresolvedFormForUse(form:string)}): void;
        getSessionId(): string;
        getWindowName(): string;
        getWindowId(): string;
        getWindowUrl(): string;
        applyBeanData(beanModel, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, componentScope:angular.IScope):void ;
        getComponentChanges(now, prev, beanConversionInfo, parentSize, property): any;
        getChangeNotifierGenerator(formName:string, beanName:string):(string);
        getFormState(name:string): angular.IPromise<FormState>;
        getFormStateWithData(name:string): angular.IPromise<FormState>;
        getFormStateEvenIfNotYetResolved(name:string): FormState;
        getFormStatesConversionInfo(): any;
		hasFormState(name:string): boolean;
        hasResolvedFormState(name:string): boolean;
        hasFormStateWithData(name:string):boolean;
        clearFormState(name:string): void;
        initFormState(formName, beanDatas, formProperties, formScope, resolve): FormState;
        updateScopeForState(formName: string, formScope: angular.IScope, state: FormState): FormState;
        resolveFormState(formName:string, skipTestResolving:boolean): FormState;
		resolveFormState(formName:string): FormState;
        unResolveFormState(formName:string): void;
        requestInitialData(formName:string, requestDataCallback:(initialFormData:any, formState:FormState)=>void): void;
        sendChanges(now, prev, formname:string, beanname:string, property:string): void;
        callService<T>(serviceName:string, methodName:string, argsObject, async:boolean): angular.IPromise<T>;
		callService<T>(serviceName:string, methodName:string, argsObject): angular.IPromise<T>;
        addToCurrentServiceCall(func:()=>void): void;
        getExecutor(formName:string):{on:(beanName:string, eventName:string, property:string, args, rowId:string)=>void};
        getLanguageAndCountryFromBrowser(): string;
        getLocale(): Locale;
        setLocale(locale:Locale): void;
        getCurrentFormUrl(fetch:boolean): string;
        setCurrentFormUrl(url:string, push:boolean): void;
	}
	
	interface ISabloConverters {
        INTERNAL_IMPL: string,
        TYPES_KEY: string,
        prepareInternalState(propertyValue, optionalInternalStateValue):void
        convertFromServerToClient(serverSentData:any, conversionInfo:any, currentClientData:any, scope:angular.IScope, modelGetter:()=>any): any,
        convertFromClientToServer(newClientData:any, conversionInfo:any, oldClientData:any): any,
        updateAngularScope(value, conversionInfo, scope:angular.IScope): void,
        registerCustomPropertyHandler(propertyTypeID:string, customHandler:{
			fromServerToClient(serverJSONValue, currentClientValue, componentScope:angular.IScope, componentModelGetter:()=>any):void,
			fromClientToServer(newClientData, oldClientData):void,
			updateAngularScope(clientValue, componentScope:angular.IScope):void
		}): void 
	}
	
	interface ISabloUtils {
			EVENT_LEVEL_SYNC_API_CALL: number,
			setCurrentEventLevelForServer(eventLevelValue:number): void,
			getCurrentEventLevelForServer():number,
			isChanged(now, prev, conversionInfo):boolean,
			getCombinedPropertyNames(now,prev): any,
			convertClientObject(value):any,
			getEventArgs(args,eventName:string):any,
			getOrCreateInDepthProperty(formStatesConversionInfo, formname:string, beanname:string):any,
			getInDepthProperty(formStatesConversionInfo, formname:string, beanname:string):any,
	}
	
	interface IWebSocket {
        connect(context:string, args, queryArgs, websocketUri:string):WSSession,
        setConnectionQueryArgument(arg:string, value:string):void,
        setConnectionPathArguments(args):void,
        getSession():WSSession,
        isConnected(): boolean,
        isReconnecting(): boolean,
		addIncomingMessageHandlingDoneTask(func: ()=>void): void,
        disconnect():void,
        getURLParameter(name:string): string
	}

}