var myPath = new Path(); 
myPath.add(new Point(20, 70)); 
myPath.add(new Point(40, 100)); 
myPath.add(new Point(100, 20)); 
 
var greenSwatch = document.swatches['CMYK Green']; 
 
myPath.fillColor = greenSwatch.color;