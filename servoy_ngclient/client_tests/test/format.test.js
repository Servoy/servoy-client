'use strict';

/* jasmine specs for filters go here */

describe('servoy $formatUtils', function() {

  beforeEach(module('servoy'));
    
  describe("format numbers", function() {
	    it("should corecly format numbers", function() {
	    	inject(function($formatterUtils){ 
	    	var formatFun = $formatterUtils.format;
	    	var MILLSIGN =  '\u2030';  //‰
	        expect(formatFun(10.49,{display:'0.000',type:'NUMBER'})).toEqual("10.490")
	        expect(formatFun(10,{display:'-0000.000',type:'NUMBER'})).toEqual("-10.000")
	        expect(formatFun(10.49,{display:'#.###',type:'NUMBER'})).toEqual("10.49")
	        expect(formatFun(10.49,{display:'+#.###',type:'NUMBER'})).toEqual("+10.49")
	        expect(formatFun(10.49,{display:'+0',type:'NUMBER'})).toEqual("+10")
	        expect(formatFun(10.49,{display:'+%00.00',type:'NUMBER'})).toEqual("+%1049.00")
	        expect(formatFun(10.49,{display: MILLSIGN+'+00.00',type:'NUMBER'})).toEqual(MILLSIGN+"+10490.00") 
	        expect(formatFun(10.49,{display: '+'+MILLSIGN+'00.00',type:'NUMBER'})).toEqual('+'+MILLSIGN+"10490.00") 
	        expect(formatFun(10.49,{display: '00.00E00',type:'NUMBER'})).toEqual('1.0490e+1');
	        
	    	})
	    });
	    it("should corecly UNformat  numbers", function() {
	    	inject(function($formatterUtils){ 
	    	var unFormatFun = $formatterUtils.unformat;
	    	var MILLSIGN =  '\u2030';  //‰
	        expect(unFormatFun("10.49",{display:'0.000',type:'NUMBER'})).toEqual(10.49)
	        expect(unFormatFun("+%1049.00",{display:'+%00.00',type:'NUMBER'})).toEqual(10.49)
	        expect(unFormatFun("-10.000",{display:'-0000.000',type:'NUMBER'})).toEqual(10)
	        expect(unFormatFun("-10.000",{display:'###.###',type:'NUMBER'})).toEqual(-10)
	        expect(unFormatFun(MILLSIGN+"+10490.00",{display:MILLSIGN+'+00.00',type:'NUMBER'})).toEqual(10.49)
	        expect(unFormatFun('1.0490e+1',{display: '00.00E00',type:'NUMBER'})).toEqual(10.49); 
	    	})
	    });
	});
  
  describe("format dates", function() {
	    it("should corecly format dates", function() {
	    	inject(function($formatterUtils){ 
	    	var formatFun = $formatterUtils.format;
	    	var MILLSIGN =  '\u2030';  //‰
	        expect(formatFun(new Date(2014,10,1,23,23,14,500),{display:'dd-MM-yyyy HH:mma s  G S',type:'DATETIME'})).toEqual("01-11-2014 23:23PM 14  AD 500")
	        expect(formatFun(new Date(2014,10,2,23,23,14),{display:'dd-MM-yyyy w HH:mma  W',type:'DATETIME'})).toEqual("02-11-2014 44 23:23PM  1")
	        expect(formatFun(new Date(2014,10,3,15,23,14),{display:'dd-MM-yyyy Z D',type:'DATETIME'})).toEqual("03-11-2014 +0200 307")
	        expect(formatFun(new Date(2014,10,4,15,23,14),{display:'dd/MM/yyyy Z D',type:'DATETIME'})).toEqual("04/11/2014 +0200 308")
	        expect(formatFun(new Date(2014,10,5,12,23,14),{display:'dd MM yyyy KK:mm D',type:'DATETIME'})).toEqual("05 11 2014 00:23 309")
	        // the following sets hour to 24:23 which is next day ,so 6'th
	        expect(formatFun(new Date(2014,10,5,24,23,14),{display:'dd MM yyyy kk:mm D',type:'DATETIME'})).toEqual("06 11 2014 24:23 310")
	        
	    	})
	    });
	});
  
  describe("format strings", function() {
	    it("should corecly format strings", function() {
	    	inject(function($formatterUtils){ 
	    	var formatFun = $formatterUtils.format;
	        // the following sets hour to 24:23 which is next day ,so 6'th
	        expect(formatFun("aa11BB22",{display:'UU##UU##',type:'TEXT'})).toEqual("AA11BB22")
	        expect(formatFun("aa11BB22",{display:'HHHHUU##',type:'TEXT'})).toEqual("AA11BB22")
	        expect(function(){formatFun("aa11BB22",{display:'#HHHUU##',type:'TEXT'})}).toThrow("input string not corresponding to format : aa11BB22 , #HHHUU##")
	    	})
	    });
	});
}); 