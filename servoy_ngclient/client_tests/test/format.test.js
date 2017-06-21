'use strict';

/* jasmine specs for filters go here */

describe('servoy $formatUtils', function() {

  beforeEach(function() {
    module('servoy');
    angular.module('pushToServerData', ['pushToServer']);
  });

  describe("format numbers", function() {
    it("should corecly format numbers", function() {
      inject(function($formatterUtils) {
        numeral.language("en")
        var formatFun = $formatterUtils.format;
        var MILLSIGN = '\u2030'; //�
        expect(formatFun(10.49, '0.000', 'NUMBER')).toEqual("10.490");
        expect(formatFun(10, '0000.000', 'NUMBER')).toEqual("0010.000");
        expect(formatFun(-10, '0000.000', 'NUMBER')).toEqual("-0010.000");
        expect(formatFun(10.49, '#.###', 'NUMBER')).toEqual("10.49");
        expect(formatFun(10.49, '+#.###', 'NUMBER')).toEqual("+10.49");
        expect(formatFun(1000, '#,###.00', 'NUMBER')).toEqual("1,000.00");
        expect(formatFun(1000, '#,###.##', 'NUMBER')).toEqual("1,000");
        expect(formatFun(12, '##-', 'NUMBER')).toEqual("12");
        expect(formatFun(-12, '##-', 'NUMBER')).toEqual("-12");
        expect(formatFun(10.49, '+0', 'NUMBER')).toEqual("+10");
        expect(formatFun(10.49, '+%00.00', 'NUMBER')).toEqual("+%1049.00");
        expect(formatFun(10.49, MILLSIGN + '+00.00', 'NUMBER')).toEqual(MILLSIGN + "+10490.00");
        expect(formatFun(10.49, '+' + MILLSIGN + '00.00', 'NUMBER')).toEqual('+' + MILLSIGN + "10490.00");
        expect(formatFun(10.49, '00.00E00', 'NUMBER')).toEqual('1.0490e+1');

      })
    });
    it("should corecly UNformat  numbers", function() {
      inject(function($formatterUtils) {
        numeral.language("en")
        var unFormatFun = $formatterUtils.unformat;
        var MILLSIGN = '\u2030'; //�
        expect(unFormatFun("10.49", '0.000', 'NUMBER')).toEqual(10.49);
        expect(unFormatFun("+%1049.00", '+%00.00', 'NUMBER')).toEqual(10.49);
        expect(unFormatFun("-10.000", '-0000.000', 'NUMBER')).toEqual(-10);
        expect(unFormatFun("-10.000", '###.###', 'NUMBER')).toEqual(-10);
        expect(unFormatFun("1,000", '#,###.00', 'NUMBER')).toEqual(1000);
        expect(unFormatFun("1,000.00", '#,###.00', 'NUMBER')).toEqual(1000);
        expect(unFormatFun("1,000.00", '#,###.##', 'NUMBER')).toEqual(1000);
        expect(unFormatFun(MILLSIGN + "+10490.00", MILLSIGN + '+00.00', 'NUMBER')).toEqual(10.49);
        expect(unFormatFun('1.0490e+1', '00.00E00', 'NUMBER')).toEqual(10.49);
      })
    });
  });

  describe("format numbers in NL", function() {
    it("should corecly format numbers", function() {
      inject(function($formatterUtils) {
        numeral.language("nl-nl")
        var formatFun = $formatterUtils.format;
        var MILLSIGN = '\u2030'; //�
        expect(formatFun(10.49, '0.000', 'NUMBER')).toEqual("10,490");
        expect(formatFun(10, '0000.000', 'NUMBER')).toEqual("0010,000");
        expect(formatFun(-10, '0000.000', 'NUMBER')).toEqual("-0010,000");
        expect(formatFun(10.49, '#.###', 'NUMBER')).toEqual("10,49");
        expect(formatFun(10.49, '+#.###', 'NUMBER')).toEqual("+10,49");
        expect(formatFun(1000, '#,###.00', 'NUMBER')).toEqual("1.000,00");
        expect(formatFun(1000, '#,###.##', 'NUMBER')).toEqual("1.000");
        expect(formatFun(10.49, '+0', 'NUMBER')).toEqual("+10");
        expect(formatFun(10.49, '+%00.00', 'NUMBER')).toEqual("+%1049,00");
        expect(formatFun(10.49, MILLSIGN + '+00.00', 'NUMBER')).toEqual(MILLSIGN + "+10490,00");
        expect(formatFun(10.49, '+' + MILLSIGN + '00.00', 'NUMBER')).toEqual('+' + MILLSIGN + "10490,00");
        expect(formatFun(10.49, '00.00E00', 'NUMBER')).toEqual('1.0490e+1'); // TODO shouldn't this also be in dutch notation??

      })
    });
    it("should corecly UNformat  numbers", function() {
      inject(function($formatterUtils) {
        numeral.language("nl-nl")
        var unFormatFun = $formatterUtils.unformat;
        var MILLSIGN = '\u2030'; //�
        expect(unFormatFun("10,49", '0.000', 'NUMBER')).toEqual(10.49);
        expect(unFormatFun("+%1049,00", '+%00.00', 'NUMBER')).toEqual(10.49);
        expect(unFormatFun("-10,000", '-0000.000', 'NUMBER')).toEqual(-10);
        expect(unFormatFun("-10,000", '###.###', 'NUMBER')).toEqual(-10);
        expect(unFormatFun("1.000", '#,###.00', 'NUMBER')).toEqual(1000);
        expect(unFormatFun("1.000,00", '#,###.00', 'NUMBER')).toEqual(1000);
        expect(unFormatFun("1.000,00", '#,###.##', 'NUMBER')).toEqual(1000);
        expect(unFormatFun(MILLSIGN + "+10490,00", MILLSIGN + '+00.00', 'NUMBER')).toEqual(10.49);
        expect(unFormatFun('1.0490e+1', '00.00E00', 'NUMBER')).toEqual(10.49); // TODO shouldn't this also be in dutch notation??
      })
    });
  });


  describe("format dates", function() {
    it("should corecly format dates", function() {
      inject(function($formatterUtils) {
        numeral.language("en");
        var formatFun = $formatterUtils.format;
        var MILLSIGN = '\u2030'; //�
        // this test depends on locale, p.m. is for nl
        
        var z = formatFun(new Date(2014, 10, 3, 15, 23, 14), 'Z', 'DATETIME');
        
        expect(formatFun(new Date(2014, 10, 1, 23, 23, 14, 500), 'dd-MM-yyyy HH:mma s  G S', 'DATETIME')).toEqual("01-11-2014 23:23p.m. 14  AD 500");
        expect(formatFun(new Date(2014, 10, 3, 15, 23, 14), 'dd-MM-yyyy Z D', 'DATETIME')).toEqual("03-11-2014 " + z + " 307"); // TODO fix timezone issues
        expect(formatFun(new Date(2014, 10, 4, 15, 23, 14), 'dd/MM/yyyy Z D', 'DATETIME')).toEqual("04/11/2014 " + z + " 308"); // TODO fix timezone issues
        //	        expect(formatFun(new Date(2014,10,3,15,23,14),'dd-MM-yyyy Z D','DATETIME')).toEqual("03-11-2014 +0200 307")// TODO fix timezone issues
        //	        expect(formatFun(new Date(2014,10,4,15,23,14),'dd/MM/yyyy Z D','DATETIME')).toEqual("04/11/2014 +0200 308")// TODO fix timezone issues
        expect(formatFun(new Date(2014, 10, 5, 12, 23, 14), 'dd MM yyyy KK:mm D', 'DATETIME')).toEqual("05 11 2014 00:23 309");
        // the following sets hour to 24:23 which is next day ,so 6'th
        expect(formatFun(new Date(2014, 10, 5, 24, 23, 14), 'dd MM yyyy kk:mm D', 'DATETIME')).toEqual("06 11 2014 24:23 310");

      })
    });
  });

  describe("format strings", function() {
    it("should corecly format strings", function() {
      inject(function($formatterUtils) {
        var formatFun = $formatterUtils.format;
        // the following sets hour to 24:23 which is next day ,so 6'th
        expect(formatFun("aa11BB22", 'UU##UU##', 'TEXT')).toEqual("AA11BB22");
        expect(formatFun("aa11BB22", 'HHHHUU##', 'TEXT')).toEqual("AA11BB22");
        expect(function() {
          formatFun("aa11BB22", '#HHHUU##', 'TEXT').toThrow("input string not corresponding to format : aa11BB22 , #HHHUU##");
        })
      });
    });
  });
});
