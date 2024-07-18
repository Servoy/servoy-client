/// <reference path="../../typings/angularjs/angular.d.ts" />
/// <reference path="../../typings/sablo/sablo.d.ts" />
/// <reference path="../../typings/jquery/jquery.d.ts" />
/// <reference path="../../typings/servoy/servoy.d.ts" />
/// <reference path="../../typings/defaults/window.d.ts" />

angular.module( 'servoyWindowManager', ['sabloApp'] )	// TODO Refactor so that window is a component with handlers
	.config( ['$locationProvider', function( $locationProvider: angular.ILocationProvider ) {
		$locationProvider.html5Mode( true );
	}] )
	.directive( 'svyWindow', function() {
		return {
			restrict: 'A',
			link: function( scope, element, attrs ) {
				var firstTimeFocus = true;
				scope['lastElementFocused'] = function( e ) {
					var firstTabIndex = parseInt( element.find( '#tabStart' ).attr( 'tabindex' ) );
					var lastTabIndex = parseInt( element.find( '#tabStop' ).attr( 'tabindex' ) );
					for(var i = firstTabIndex + 1; i < lastTabIndex; i++) {
						var newTarget = $( '[tabindex=' + i +']' );
						// if there is no focusable element in the window, then newTarget == e.target,
						// do a check here to avoid focus cycling
						if(newTarget.is(":visible") && (e.target != newTarget[0])) {
							newTarget.focus();
							firstTimeFocus = false;
							break;
						}
					}
				}

				scope['firstElementFocused'] = function( e ) {
					var firstTabIndex = parseInt( element.find( '#tabStart' ).attr( 'tabindex' ) );
					var lastTabIndex = parseInt( element.find( '#tabStop' ).attr( 'tabindex' ) );
					if (firstTimeFocus === true) {						
						for(var i = firstTabIndex + 1; i < lastTabIndex; i++) {
							var newTarget = $( '[tabindex=' + i + ']' );
							// if there is no focusable element in the window, then newTarget == e.target,
							// do a check here to avoid focus cycling
							if(newTarget.is(":visible") && (e.target != newTarget[0])) {
								newTarget.focus();
								firstTimeFocus = false;
								break;
							}
						}
					} else {
						for(var i = lastTabIndex - 1; i > firstTabIndex; i--) {
							var newTarget = $( '[tabindex=' + i + ']' );
							// if there is no focusable element in the window, then newTarget == e.target,
							// do a check here to avoid focus cycling
							if(newTarget.is(":visible") && (e.target != newTarget[0])) {
								newTarget.focus();
								firstTimeFocus = false;
								break;
							}
						}
					}
				}
			}
		};
	} )
	.factory( '$servoyWindowManager', ['$timeout', '$rootScope', '$http', '$q', '$templateCache', '$injector', '$controller', '$compile', 'WindowType',
		function( $timeout: angular.ITimeoutService, $rootScope: angular.IRootScopeService, $http: angular.IHttpService, $q: angular.IQService, $templateCache: angular.ITemplateCacheService, $injector, $controller: angular.IControllerService, $compile: angular.ICompileService, WindowType: servoy.WindowType ) {
			var WM = new window.WindowManager();
			var self: servoy.IServoyWindowManager = {
				BSWindowManager: WM,
				instances: {},
				open: function( windowOptions ) {
					var dialogOpenedDeferred: ng.IDeferred<boolean> = $q.defer();

					//prepare an instance of a window to be injected into controllers and returned to a caller
					var windowInstance = windowOptions.windowInstance;

					//merge and clean up options
					windowOptions.resolve = windowOptions.resolve || {};
					//verify options
					if ( !windowOptions.template && !windowOptions.templateUrl ) {
						throw new Error( 'One of template or templateUrl options is required.' );
					}

					// wait for templateURL and resolve options
					var templateAndResolvePromise =
						$q.all( [getTemplatePromise( windowOptions )].concat( getResolvePromises( windowOptions.resolve ) ) );

					templateAndResolvePromise.then( function( tplAndVars: any ) {
						//initialize dialog scope and controller
						var windowScope = ( windowOptions.scope || $rootScope ).$new();
						//				windowScope['$close'] = windowInstance['close']; // TODO will WindowInstance ever have a close or dismiss function??
						//				windowScope['$dismiss'] = windowInstance['dismiss'];
						windowInstance.$scope = windowScope;

						var ctrlLocals = {};
						var resolveIter = 1;

						//controllers
						if ( windowOptions.controller ) {
							ctrlLocals['$scope'] = windowScope;
							ctrlLocals['windowInstance'] = windowInstance;
							angular.forEach( windowOptions.resolve, function( value, key ) {
								ctrlLocals[key] = tplAndVars[resolveIter++];
							} );

							$controller( windowOptions.controller, ctrlLocals );
						}
						var isModal = ( ctrlLocals['windowInstance']['type'] == WindowType.MODAL_DIALOG );

						//resolve initial bounds
						var location = null;
						var size = windowInstance.form.size;
						if ( windowInstance.initialBounds ) {
							var bounds = windowInstance.initialBounds;
							if (bounds.x > -1 && bounds.y > -1) {
								location = {
									x: bounds.x,
									y: bounds.y
								};
							}
							if  (bounds.width > 0 && bounds.height > 0) {
								size = { width: bounds.width, height: bounds.height }
							}
						}
						if ( windowInstance.location ) {
							location = windowInstance.location;
						}
						if ( windowInstance.size ) {
							size = windowInstance.size;
						}
						//-1 means default size and location(center)
						var formSize = size;
						if ( !formSize || ( formSize.width === -1 && formSize.height === -1 ) )
							formSize = windowInstance.form.size;
						//this can happen in case of responsive forms
						if ( formSize.width == 0 ) formSize.width = $( window ).width() / 2;
						if ( formSize.height == 0 ) formSize.height = $( window ).height() / 2;


						if ( !location || ( location.x < 0 && location.y < 0 ) ) location = centerWindow( formSize );
						if ( !size || size.width < 0 || size.height < 0 ) size = null;

						if ( size ) {
							// dialog shouldn't be bigger than viewport
							var browserWindow = $( window );
							if ( size.width && size.width > browserWindow.width() ) {
								size.width = browserWindow.width();
							}
							if ( size.height && size.height > browserWindow.height() ) {
								size.height = browserWindow.height();
							}
						}
						//convert servoy x,y to library top , left
						var loc = { left: location.x, top: location.y }

						var compiledWin = $compile( tplAndVars[0] )( windowScope );
						//create the bs window instance
						var win = WM.createWindow( {
							id: windowInstance.name,
							fromElement: compiledWin,
							title: "Loading...",
							resizable: !!windowInstance.resizable,
							location: loc,
							size: size,
							isModal: isModal
						} )

						//set servoy managed bootstrap-window Instance
						windowInstance.bsWindowInstance = win;
					}, function resolveError( reason ) {
						dialogOpenedDeferred.reject( reason );
					} );

					//notify dialog opened or error
					templateAndResolvePromise.then( function() {
						dialogOpenedDeferred.resolve( true );
					}, function() {
						dialogOpenedDeferred.reject( false );
					} );

					return dialogOpenedDeferred.promise;
				},
				destroyAllDialogs: function() {
					for ( var dialog in self.instances ) {
						if ( self.instances[dialog] && self.instances[dialog].bsWindowInstance ) self.instances[dialog].hide();
					}
					self.instances = {};
				}
			}


			//	utiliy functions
			function getTemplatePromise( options: { template?: string, templateUrl?: string } ) {
				return options.template ? $q.when( options.template ) :
					$http.get( options.templateUrl, { cache: $templateCache } ).then( function( result ) {
						return result.data;
					} );
			}

			function getResolvePromises( resolves ) {
				var promisesArr = [];
				angular.forEach( resolves, function( value ) {
					if ( angular.isFunction( value ) || angular.isArray( value ) ) {
						promisesArr.push( $q.when( $injector.invoke( value ) ) );
					}
				} );
				return promisesArr;
			}
			function centerWindow( formSize ) {
				var body = $( 'body' );
				var browserWindow = $( window );
				var top, left,
					bodyTop = body.position().top + parseInt( body.css( 'paddingTop' ), 10 );
				left = ( browserWindow.width() / 2 ) - ( formSize.width / 2 );
				top = ( browserWindow.height() / 2 ) - ( formSize.height / 2 );
				if ( top < bodyTop ) {
					top = bodyTop;
				}
				if ( left < 0 ) left = 0;
				if ( top < 0 ) top = 0;
				return { x: left, y: top }
			};
			return self;

		}] ).factory( "$windowService", function( $servoyWindowManager: servoy.IServoyWindowManager, $log: sablo.ILogService, $rootScope: servoy.IRootScopeService, $solutionSettings: servoy.SolutionSettings, $window: angular.IWindowService, $timeout: angular.ITimeoutService, $formService, $sabloApplication: sablo.ISabloApplication, webStorage, WindowType: servoy.WindowType, $servoyInternal, $templateCache: angular.ITemplateCacheService, $location: angular.ILocationService, $sabloLoadingIndicator, $sabloTestability, $svyUIProperties ) {
			var instances = $servoyWindowManager.instances;
			var formTemplateUrls: { [s: string]: string; } = {};
			var storage = webStorage.local;
			var sol = $solutionSettings.solutionName + '.';
			var windowCounter = 0;

			// track main app window size change
			var mwResizeTimeoutID;
			$window.addEventListener( 'resize', function() {
				if ( mwResizeTimeoutID ) $timeout.cancel( mwResizeTimeoutID );
				mwResizeTimeoutID = $timeout( function() {
					$sabloApplication.callService( "$windowService", "resize", { size: { width: $window.innerWidth, height: $window.innerHeight } }, true );
				}, 500 );
			} );

			function getFormUrl( formName: string ): string {
				var realFormUrl = formTemplateUrls[formName];
				if ( realFormUrl == null || realFormUrl == undefined ) {
					formTemplateUrls[formName] = "";
					$sabloApplication.callService( "$windowService", "touchForm", { name: formName }, true );
				}
				else if ( realFormUrl.length == 0 ) {
					// waiting for updateForm to come
					return null;
				}
				return realFormUrl;
			}

			function prepareFormForUseInHiddenDiv( formName ) {
				// the code should work even if we remove all the following timeouts, just execute directly - but these are meant as an optimization for the common cases
				$timeout( function() { // $timeout (a random number of multiple ones) are used to try to avoid cases in which a component already will use the template URL in which case we avoid loading it in hidden div unnecessarily
					$timeout( function() {
						$timeout( function() {
							$timeout( function() {
								if ( $log.debugEnabled ) $log.debug( "svy * checking if prepareFormForUseInHiddenDiv still needs to do something (form isn't already loaded elsewhere): " + formName );
								if ( !$sabloApplication.hasResolvedFormState( formName ) ) {
									// in order to call web component API's for example we will create appropriate DOM and create the directives/scopes (but hidden) so that API call doesn't go to destroyed web component...
									var formURL = formTemplateUrls[formName];

									// the form URL should be already there (sync/async api calls or other things that require forms to be loaded somewhere usually also do touchForm which populates the URL beforehand);
									// the exception here is delayed-until-form-load API calls (like requestFocus) that will not do touchForm but those shouldn't call prepareFormForUseInHiddenDiv anyway, they just wait for the form to get loaded by someone else
									if ( formURL && formURL.length > 0 ) {
										$rootScope.updatingFormUrl = formURL;
										$sabloApplication.getFormState( formName ).then( function( formState ) {
											// if first show of this form in browser window then request initial data (dataproviders and such);
											if ( formState.initializing && !formState.initialDataRequested ) $servoyInternal.requestInitialData( formName, formState );
										} );
									} else {
										$log.error( "svy * Trying to load form in hidden, but form URL is empty (not yet prepared); forcing reload... " + formName );
										$rootScope.updatingFormUrl = getFormUrl( formName ); // will still be null, but it will call a force touch on server that should end up loading it in hidden... see updateController
									}
									if ( $log.debugEnabled ) $log.debug( "svy * $rootScope.updatingFormUrl = " + $rootScope.updatingFormUrl + " [prepareFormForUseInHiddenDiv - " + formName + "]" );
									$rootScope.updatingFormName = formName;
								}
							}, 0 );
						}, 0 );
					}, 0 );
				}, 0 );
			}

			$sabloApplication.contributeFormResolver( {

				// makes sure the given form is prepared (so DOM/directives are ready for use, not necessarily with initial data)
				prepareUnresolvedFormForUse: prepareFormForUseInHiddenDiv

			} );

			$rootScope.$watch( function() { return $location.url(); }, function( newURL, oldURL ) {
				if ( newURL != oldURL ) {
					var formName = $location.hash();
					if ( formName && formName != $solutionSettings.mainForm.name ) {
						$formService.goToForm( formName );
					}
				}
			} );

			function saveInSessionStorage(property, propertyName) {
				const currentWindow = 'window' + windowCounter;
				if (property && webStorage.session.has(currentWindow)) {
					let window = webStorage.session.get(currentWindow);
					if (!window[propertyName]) {
						window[propertyName] = property;
						webStorage.session.set(currentWindow, window);
					}
				}
			}

			var self: servoy.IWindowService = {

				getLoadedFormState: function() {
					var loadedState: { [s: string]: {url:string,attached:boolean}; } = {};
					for ( var formName in formTemplateUrls ) {
						if ( formName ) {
							var state = $sabloApplication.getFormStateEvenIfNotYetResolved( formName );
							if ( state ) { // can this be null if form name is there?
								loadedState[formName] = {url:formTemplateUrls[formName],attached:state.getScope != null && state.getScope != undefined};
							}
							else {
								loadedState[formName] = {url:formTemplateUrls[formName],attached:false};
							}
						}
					}
					return loadedState
				},
				create: function( name, type ) {
					webStorage.session.set('window' + windowCounter, {
						name: name,
						type: type
					});
					// dispose old one
					if ( instances[name] ) {

					}
					if ( !instances[name] ) {
						var win =
							{
								name: name,
								type: type,
								title: '',
								opacity: 1,
								undecorated: false,
								cssClassName: null,
								size: null,
								location: null,
								navigatorForm: null,
								form: null,
								initialBounds: null,
								resizable: false,
								transparent: false,
								storeBounds: false,
								bsWindowInstance: null,  // bootstrap-window instance , available only after creation
								$scope: null,
								hide: function() {
									if ( win.bsWindowInstance ) win.bsWindowInstance.close();
									if ( !this.storeBounds ) {
										delete this.location;
										delete this.size;
									}
									delete win.bsWindowInstance
									if ( win.$scope ) win.$scope.$destroy();
								},
								setLocation: function( location ) {
									this.location = location;
									if ( win.bsWindowInstance ) {
										win.bsWindowInstance.$el.css( 'left', this.location.x + 'px' );
										win.bsWindowInstance.$el.css( 'top', this.location.y + 'px' );
									}
									if ( this.storeBounds ) storage.set( sol + name + '.storedBounds.location', location )
								},
								setSize: function( size ) {
									this.size = size;
									if ( win.bsWindowInstance ) {
										win.bsWindowInstance.setSize( size );
									}
									if ( this.storeBounds ) storage.set( sol + name + '.storedBounds.size', size )
								},
								getSize: function() {
									return win.size;
								},
								onResize: function( $event, size ) {
									win.size = size;
									if ( win.storeBounds ) storage.set( sol + name + '.storedBounds.size', size )
									$sabloApplication.callService( "$windowService", "resize", { name: win.name, size: win.size }, true );
									win['$scope'].$broadcast( "dialogResize" );
								},
								onMove: function( $event, location ) {
									win.location = { x: location.left, y: location.top };
									if ( win.storeBounds ) storage.set( sol + name + '.storedBounds.location', win['location'] )
									$sabloApplication.callService( "$windowService", "move", { name: win.name, location: win['location'] }, true );
								},
								toFront: function() {
									$servoyWindowManager.BSWindowManager.setFocused( this.bsWindowInstance )
								},
								toBack: function() {
									$servoyWindowManager.BSWindowManager.sendToBack( this.bsWindowInstance )
								},
								clearBounds: function() {
									storage.remove( sol + name + '.storedBounds.location' )
									storage.remove( sol + name + '.storedBounds.size' )
								}
							};

						instances[name] = win;
						return win;
					}

				},
				show: function( name, form, title ) {
                    const currentWindow = 'window' + windowCounter;
					if (webStorage.session.has(currentWindow)) {
						let window = webStorage.session.get(currentWindow);
						window.showForm = form;
						window.showTitle = title;
						webStorage.session.set(currentWindow, window);
						windowCounter++;
					}
					var instance = instances[name];
					if ( instance ) {
						instance.title = title;
						if ( instance.bsWindowInstance ) {
							// already showing
							return;
						}
						if ( $( document ).find( '[svy-window]' ).length < 1 ) {
							$( "#mainForm" ).trigger( "disableTabseq" );
						}
						if ( instance.storeBounds ) {
							instance.size = storage.get( sol + name + '.storedBounds.size' )
							instance.location = storage.get( sol + name + '.storedBounds.location' )
						}
						$sabloTestability.block( true );
						$servoyWindowManager.open( {
							//					animation: false,
							templateUrl: "templates/dialog.html",
							controller: "DialogInstanceCtrl",
							//					windowClass: "tester",
							windowInstance: instance
						} ).then( function() {
							// test if it is modal dialog, then the request blocks on the server and we should hide the loading.
							if ( instance.type == 1 && $sabloLoadingIndicator.isShowing() ) {
								instance['loadingIndicatorIsHidden'] = 0;
								// as long as the indicator says it is still showing call hide loading to
								// get the loading counter really to 0
								// this happens a second modal dialog is showing in the hide of another...
								// then the stack on the server can't rewind, because it is still in the stack of the first dialog
								while ( $sabloLoadingIndicator.isShowing() ) {
									instance['loadingIndicatorIsHidden']++;
									$sabloLoadingIndicator.hideLoading();
								}
							}
							$sabloTestability.increaseEventLoop();
							instance.bsWindowInstance.$el.on( 'bswin.resize', instance.onResize )
							instance.bsWindowInstance.$el.on( 'bswin.move', instance.onMove )
							instance.bsWindowInstance.$el.on( "bswin.active", function( ev, active ) {
								$( ev.currentTarget ).trigger( active ? "enableTabseq" : "disableTabseq" );
							} );
							instance.bsWindowInstance.$el.find( ".window-header" ).focus();
							instance.bsWindowInstance.setActive( true );
							// init the size of the dialog
							var width = instance.bsWindowInstance.$el.width();
							var height = instance.bsWindowInstance.$el.height();
							if ( width > 0 && height > 0 ) {
								var dialogSize = { width: width, height: height };
								$sabloApplication.callService( "$windowService", "resize", { name: instance.name, size: dialogSize }, true );
							}
						}, function( reason ) {
							throw reason;
						} )
						if ( ($solutionSettings.windowName == name &&  $solutionSettings.mainForm.name != form) || ($solutionSettings.windowName != name && instance.form.name != form) ) $log.error( "switchform should set the instances state before showing it: '" + form + "'" );
					}
					else {
						$log.error( "Trying to show window with name: '" + name + "' which is not created." );
					}
				},
				hide: function( name ) {
					var winCounter = 0;
					while(webStorage.session.has('window' + winCounter)) {
						let window = webStorage.session.get('window' + winCounter);
						if (window.name == name) {
							webStorage.session.remove('window' + winCounter);
							windowCounter--;
						} 
						winCounter++;
					} 
					var instance = instances[name];
					if ( instance ) {
						if ( instance['loadingIndicatorIsHidden'] ) {
							var counter = instance['loadingIndicatorIsHidden'];
							delete instance['loadingIndicatorIsHidden'];
							while ( counter-- > 0 ) {
								$sabloLoadingIndicator.showLoading();
							}
						}
						$sabloTestability.decreaseEventLoop();
						instance.hide();
						if ( $( document ).find( '[svy-window]' ).length < 1 ) {
							$( "#mainForm" ).trigger( "enableTabseq" );
						}
					} else {
						$log.error( "Trying to hide window : '" + name + "' which is not created. If this is due to a developer form change/save while dialog is open in client it is expected." );
					}
				},
				destroy: function( name ) {
					var instance = instances[name];
					if ( instance ) {
						delete instances[name];
					} else {
						$log.error( "Trying to destroy window : '" + name + "' which is not created. If this is due to a developer form change/save while dialog is open in client it is expected." );
					}
				},
				switchForm: function( name, form, navigatorForm ) {
					const currentWindow = 'window' + windowCounter;
					if (webStorage.session.has(currentWindow)) {
						let window = webStorage.session.get(currentWindow);
						if (!window.switchForm) {
							window.switchForm = form;
							window.navigatorForm = navigatorForm;
							webStorage.session.set(currentWindow, window);
						}
					}
					// if first show of this form in browser window then request initial data (dataproviders and such)
					$formService.formWillShow( form.name, false ); // false because form was already made visible server-side
					if ( navigatorForm && navigatorForm.name && navigatorForm.name.lastIndexOf( "default_navigator_container.html" ) == -1 ) {
						// if first show of this form in browser window then request initial data (dataproviders and such)
						$formService.formWillShow( navigatorForm.name, false ); // false because form was already made visible server-side
					}

					if ( instances[name] && instances[name].type != WindowType.WINDOW ) {
						instances[name].form = form;
						instances[name].navigatorForm = navigatorForm;
					} else if ( $solutionSettings.windowName == name ) { // main window form switch
						$solutionSettings.mainForm = form;
						$solutionSettings.navigatorForm = navigatorForm;

						if ($svyUIProperties.getUIProperty("servoy.ngclient.formbased_browser_history") !== false ) {
							$location.hash(form.name)
			            }
					}
					if ( !$rootScope.$$phase ) {
						if ( $log.debugLevel === $log.SPAM ) $log.debug( "svy * Will call digest from switchForm for root scope" );
						$rootScope.$digest();
					}
				},
				setTitle: function( name, title ) {
					saveInSessionStorage(title, 'title');
					if ( instances[name] && instances[name].type != WindowType.WINDOW ) {
						instances[name].title = title;
					} else {
						$solutionSettings.solutionTitle = title;
						if ( !$rootScope.$$phase ) {
							if ( $log.debugLevel === $log.SPAM ) $log.debug( "svy * Will call digest from setTitle for root scope" );
							$rootScope.$digest();
						}
					}
				},
				setInitialBounds: function( name, initialBounds ) {
					saveInSessionStorage(initialBounds, 'initialBounds');
					if ( instances[name] ) {
						instances[name].initialBounds = initialBounds;
					}
				},
				setStoreBounds: function( name, storeBounds ) {
					saveInSessionStorage(storeBounds, 'storeBounds');
					if ( instances[name] ) {
						instances[name].storeBounds = storeBounds;
					}
				},
				resetBounds: function( name ) {
					if ( instances[name] ) {
						instances[name].storeBounds = false;
						instances[name].clearBounds()
					}
				},
				setLocation: function( name, location ) {
					saveInSessionStorage(location, 'location');
					if ( instances[name] ) {
						instances[name].setLocation( location );
					}
				},
				setSize: function( name, size ) {
					saveInSessionStorage(size, 'size');
					if ( instances[name] ) {
						instances[name].setSize( size );
					}
				},
				getSize: function( name ) {
					if ( instances[name] && instances[name].bsWindowInstance ) {
						return instances[name].getSize();
					}
					else {
						return { width: $window.innerWidth, height: $window.innerHeight }
					}
				},
				setUndecorated: function( name, undecorated ) {
					saveInSessionStorage(undecorated, 'undecorated');
					if ( instances[name] ) {
						instances[name].undecorated = undecorated;
					}
				},
				setCSSClassName: function( name, cssClassName ) {
					saveInSessionStorage(cssClassName, 'cssClassName');
					const currentWindow = 'window' + windowCounter;
					if (webStorage.session.has(currentWindow)) {
						let window = webStorage.session.get(currentWindow);
						if (!window.cssClassName) {
							window.cssClassName = cssClassName;
							webStorage.session.set(currentWindow, window);
						}
					}
					if ( instances[name] ) {
						instances[name].cssClassName = cssClassName;
					}
				},
				setOpacity: function( name, opacity ) {
					saveInSessionStorage(opacity, 'opacity');
					if ( instances[name] ) {
						instances[name].opacity = opacity;
					}
				},
				setResizable: function( name, resizable ) {
					saveInSessionStorage(resizable, 'resizable');
					if ( instances[name] ) {
						instances[name].resizable = resizable;
					}
				},
				setTransparent: function( name, transparent ) {
					saveInSessionStorage(transparent, 'transparent');
					if ( instances[name] ) {
						instances[name].transparent = transparent;
					}
				},
				toFront: function( name ) {
					if ( instances[name] ) {
						instances[name].toFront();
					}
				},
				toBack: function( name ) {
					if ( instances[name] ) {
						instances[name].toBack();
					}
				},
				reload: function() {
					$window.location.reload( );
				},
				updateController: function( formName, controllerCode, realFormUrl, forceLoad, html ) {
					if ( $log.debugEnabled ) $log.debug( "svy * updateController = " + formName + ", realFormUrl = " + realFormUrl );
					if ( formTemplateUrls[formName] !== realFormUrl ) {
						if ( formTemplateUrls[formName] ) {
							$templateCache.remove( formTemplateUrls[formName] );
						}
						if ( html ) $templateCache.put( realFormUrl, html );
						var formState = $sabloApplication.getFormStateEvenIfNotYetResolved( formName );
						$sabloApplication.clearFormState( formName )
						evalControllerCodeWithoutClosure( controllerCode );
						formTemplateUrls[formName] = realFormUrl;

						// if the form was already initialized and visible, then make sure it is reinitialized
						if ( formState && formState.getScope != undefined ) {
							$sabloApplication.getFormState( formName ).then( function( formState ) {
								if ( $log.debugEnabled ) $log.debug( "svy * updateController; checking to see if requestInitialData is needed = " + formName + " (" + formState.initializing + ", " + formState.initialDataRequested + ")" );
								if ( formState.initializing && !formState.initialDataRequested ) $servoyInternal.requestInitialData( formName, formState );
							} );
						}

						// TODO can this be an else if the above if? will it always force load anyway?
						// getFormURL can force a touch when it needs to be shown in hidden div - that is why we put it in updatingFormUrl here; otherwise we'd need a promise-based formURL impl
						if ( forceLoad ) {
							$rootScope.updatingFormUrl = realFormUrl;
							$rootScope.updatingFormName = formName;
							if ( $log.debugEnabled ) $log.debug( "svy * $rootScope.updatingFormUrl = " + $rootScope.updatingFormUrl + " [updateController FORCED - " + formName + "]" );
							$sabloApplication.getFormState( formName ).then( function( formState ) {
								// if first show of this form in browser window then request initial data (dataproviders and such);
								if ( formState.initializing && !formState.initialDataRequested ) $servoyInternal.requestInitialData( formName, formState );
							} );
						}
						if ( !$rootScope.$$phase ) {
							if ( $log.debugLevel === $log.SPAM ) $log.debug( "svy * Will call digest from updateController for root scope" );
							$rootScope.$digest();
						}
					} else if ( $log.debugEnabled ) {
						$log.warn( "svy * updateController for form '" + formName + "' was ignored as the URLs are identical and we don't want to clear all kinds of states/caches without the form getting reloaded due to URL change" );
					}
				},
				requireFormLoaded: function( formName ) {
					// in case updateController was called for a form before with forceLoad == false, the form URL might not really be loaded by the bean that triggered it
					// because the bean changed it's mind, so when a new server side touchForm() comes for this form with forceLoad == true then we must make sure the
					// form URL is used to create the directives/DOM and be ready for use
					if ( $log.debugEnabled ) $log.debug( "svy * requireFormLoaded: " + formName );
					prepareFormForUseInHiddenDiv( formName );
					if ( !$rootScope.$$phase ) {
						if ( $log.debugLevel === $log.SPAM ) $log.debug( "svy * Will call digest from requireFormLoaded for root scope" );
						$rootScope.$digest();
					}
				},
				destroyController: function( formName ) {
					$sabloApplication.clearFormState( formName );
					delete formTemplateUrls[formName];
					if ( $solutionSettings.mainForm.name == formName ) {
						$solutionSettings.mainForm.name = undefined;
					}
				},
				getFormUrl: getFormUrl
			}
			return self;

		} ).value( 'WindowType', {
			DIALOG: 0,
			MODAL_DIALOG: 1,
			WINDOW: 2
		} ).controller( "DialogInstanceCtrl", function( $scope: angular.IScope & { win: servoy.WindowInstance, getFormUrl(): string, getNavigatorFormUrl(): string, isUndecorated(): boolean, cancel(): void },
			windowInstance: servoy.WindowInstance, $timeout: angular.ITimeoutService, $windowService: servoy.IWindowService, $servoyInternal, $sabloApplication: sablo.ISabloApplication, $formService, $sabloTestability ) {

			var block = true;
			// these scope variables can be accessed by child scopes
			// for example the default navigator watches 'win' to see if it changed the current form
			$scope.win = windowInstance
			$scope.getFormUrl = function() {
				var url = $windowService.getFormUrl( windowInstance.form.name )
				if ( block && url ) {
					$sabloTestability.block( false );
					block = false;
				}
				return url;

			}
			$scope.getNavigatorFormUrl = function() {
				if ( windowInstance.navigatorForm && windowInstance.navigatorForm.name ) {
					if ( windowInstance.navigatorForm.name.lastIndexOf( "default_navigator_container.html" ) > 0 ) {
						return windowInstance.navigatorForm.name;
					}
					return $windowService.getFormUrl( windowInstance.navigatorForm.name );
				}
				return null;
			}

			$scope.isUndecorated = function() {
				return $scope.win.undecorated || ( $scope.win.opacity < 1 )
			}

			$formService.formWillShow( windowInstance.form.name, false );

			$scope.cancel = function() {
				/*var promise = */$sabloApplication.callService( "$windowService", "windowClosing", { window: windowInstance.name }, false );
				// close is handled server side
				//		promise.then(function(ok) {
				//			if (ok) {
				//				$windowService.hide(windowInstance.name);
				//			}
				//		})
			};
		} ).directive( 'unwantedDialogScrollbarsWorkaround', function( $log: sablo.ILogService, $timeout: angular.ITimeoutService ) {
			return {
				restrict: 'A',

				link: function( $scope, $element ) {
					// workaround for unneeded scrollbars appearing in dialogs in Chrome (although sizes seem ok and just toggling on and off overflow: auto in the form-in-dialog body part makes them go away) - see SVY-9172;
					// the workaround uses some obscure JS code that force relayout of the dialog (hide, get width, show); as it will do hide an show in the same browser event cycle one after the other, there's no risk that
					// api calls on that form (such as say a requestFocus) can get executed while content is hidden temporarily and misbehave... 
					$timeout( function() { // TODO shouldn't this be done when we know form contents are shown rather then rely on this timeout?
						if ( $log.debugEnabled ) $log.debug( "svy * Unneeded scrollbars in dialog workaround is being applied... " );
						var oldDisplay = $element.css( 'display' );
						var focusedChild = $element.find( ":focus" );

						$element.css( 'display', 'none' );
						$element.height();
						$element.css( 'display', oldDisplay );

						try {
							if ( $log.debugEnabled && focusedChild.length > 0 ) $log.debug( "svy * Restoring focus after unneded scrollbars workaround execution... Fe: " + focusedChild.length );
							focusedChild.focus(); // restore child element focus if needed (focusedChild is a jQuery collection that will have either 1 elements or 0 elements - if a child was focused or not)
						} catch ( ie ) {
							/* docs say IE can end up throwing errors in a scenario that in not likely to happen here; but anyway avoid that */
							if ( $log.debugEnabled ) $log.debug( "svy * Restoring focus after unneded scrollbars workaround execution failed. Error: " + ie );
						}
					}, 0 );
				} 
			} 
		} ).run(function($sabloApplication, $windowService: servoy.IWindowService, $webSocket ,webStorage, $solutionSettings) {   
			
			// the window must have a form to show
	        if (webStorage.session.has('window0') && webStorage.session.get('window0').showForm != undefined) { 
	        	  	let isConnected = false;
					// wait until the server is connected
        	  		let interval = setInterval(() => {
        	  			if ($webSocket.isConnected()) {
        	  				restoreDialogs();
        	  			}
        	  		}, 1000);
        	  		function restoreDialogs() {  
        	  			clearInterval(interval);
    					let counter = 0;
    					while(webStorage.session.has('window' + counter)) {
    						let window = webStorage.session.get('window' + counter);
                            // do not restore main window
                            if ($solutionSettings.windowName != window.name){
							 // call a couple of methods that will create and display the window
        	                   $windowService.create(window.name, window.type);
        	                   $windowService.switchForm(window.name, window.switchForm, window.navigatorForm);
        	                   $windowService.setTitle(window.name, window.title);
        	                   $windowService.setUndecorated(window.name, window.undecorated);
							   $windowService.setCSSClassName(window.name, window.cssClassName);
							   $windowService.setSize(window.name, window.size);
							   $windowService.setInitialBounds(window.name, window.initialBounds);
							   $windowService.setStoreBounds(window.name, window.storeBounds);
							   $windowService.setLocation(window.name, window.location);
							   $windowService.setOpacity(window.name, window.opacity);
							   $windowService.setTransparent(window.name, window.transparent);
        	                  $windowService.show(window.name, window.showForm, window.showTitle);
                            }
    						counter++;
    					} 
        	  		}
	            }
		});
 
function evalControllerCodeWithoutClosure(controllerCode) {
	if (controllerCode) {
		eval(controllerCode);
	}
}
