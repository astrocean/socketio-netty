var socket;
// The ID of the room users will join in order to draw together
var roomID = "demo.room";
var currentClientId = new Date().getTime();
// A hash of client attribute names used in this application. Each client sets a
// "thickness" attribute and a "color" attribute, specify the thickness and 
// color of the current line being drawn.
var Attributes = {THICKNESS:"thickness", 
                  COLOR:"color"};
// A hash of room message names used in this application. MOVE means move the
// drawing pen to the specified position. PATH supplies a list of points to be
// drawn.
var Messages = {MOVE:"MOVE", 
                PATH:"PATH"};

//==============================================================================
// LOCAL USER VARIABLES
//==============================================================================
// A flag to track whether the user is drawing or not
var isPenDown = false;

// Line defaults
var defaultLineColor = "#AAAAAA";
var defaultLineThickness = 1;
var maxLineThickness = 30;

// Tracks the current location of the user's drawing pen
var localPen = {};

// The user's line styles 
var localLineColor = defaultLineColor;
var localLineThickness = defaultLineThickness;

// A list of points in a path to send to other connected users
var bufferedPath = [];
// A timestamp indicating the last time a point was added to the bufferedPath
var lastBufferTime = new Date().getTime();

//==============================================================================
// REMOTE USER VARIABLES
//==============================================================================
// A hash of pen positions for remote users, in the following 
// format ("2345" is an example client ID):
//  {"2345": {x:10, y:10}}
var userCurrentPositions = {};
// A hash of pending drawing commands sent by remote users, the following format: 
//  {"2345": [{commandName:moveTo, arg:{x:10, y:10}}, {commandName:lineTo, arg:{x:55, y:35}}]};
var userCommands = {};
// A hash of line colors for remote users, in the following format:
//  {"2345": "#CCCCCC"};
var userColors = {};
// A hash of line thicknesses for remote users, in the following format:
//  {"2345": 5};
var userThicknesses = {};

//==============================================================================
// DRAWING VARIABLES
//==============================================================================
// The HTML5 drawing canvas
var canvas;
// The drawing canvas's context, through which drawing commands are performed
var context;
// A hash of drawing commands executed by server's rendering process
var DrawingCommands = {LINE_TO:       "lineTo",
                       MOVE_TO:       "moveTo",
                       SET_THICKNESS: "setThickness",
                       SET_COLOR:     "setColor"};

//==============================================================================
// TIMER VARIABLES
//==============================================================================
// The ID for a timer that sends the user's drawing path on a regular interval
var broadcastPathIntervalID;
// The ID for a timer that executes drawing commands sent by remote users
var processDrawingCommandsIntervalID;

//==============================================================================
// TOUCH-DEVICE VARIABLES
//==============================================================================
var hasTouch = false;

var isShape = false;
var shapeObj = {};

//==============================================================================
// INITIALIZATION
//==============================================================================
// Trigger init() when the document finishes loading
window.onload = init;

// Main initialization function
function init () {
  initCanvas();
  registerInputListeners();
  initOrbiter();
  iPhoneToTop();
  
  setStatus("Connecting to Server...");
}

// Set up the drawing canvas
function initCanvas () {
  // Retrieve canvas reference
  canvas = document.getElementById("canvas");
  
  // If IE8, do IE-specific canvas initialization (required by excanvas.js)
  if (typeof G_vmlCanvasManager != "undefined") {
    this.canvas = G_vmlCanvasManager.initElement(this.canvas);
  }
  
  // Size canvas
  canvas.width  = 600;
  canvas.height = 400;
  
  // Retrieve context reference, used to execute canvas drawing commands
  context = canvas.getContext('2d');
  context.lineCap = "round";
  
  // Set control panel defaults
  document.getElementById("thickness").selectedIndex = 0;
  document.getElementById("color").selectedIndex = 1;
}

// Register callback functions to handle user input
function registerInputListeners () {
  canvas.onmousedown = pointerDownListener;
  document.onmousemove = pointerMoveListener;
  document.onmouseup = pointerUpListener;
  
  document.ontouchstart = touchDownListener;
  document.ontouchmove = touchMoveListener;
  document.ontouchend = touchUpListener;
  
  document.getElementById("thickness").onchange = thicknessSelectListener;
  document.getElementById("color").onchange = colorSelectListener;

  document.getElementById("shape").onchange = shapeSelectListener;
}

// Initialize Orbiter, which handles multiuser communications
function initOrbiter () {
   //socket = io.connect('http://localhost:9000');
}
socket = io.connect('http://10.95.20.172:9000');
socket.on('connect', function() {
	socket.emit('roomNotice', {room:roomID, client:currentClientId});

	//roomOccupantCountUpdateListener (roomID, numOccupants)
	processDrawingCommandsIntervalID = setInterval(processDrawingCommands, 20);
	//$('#chat').addClass('connected');
});

socket.on('disconnect', function() {
  setStatus("Disconnected from Server.");
  // Stop drawing content sent by other users
  clearInterval(processDrawingCommandsIntervalID);
});

socket.on('clearCanvas', function(obj){
	if(!obj.points)
		context.clearRect(0, 0, canvas.width, canvas.height);
	else{
//		alert("clear rect");
		console.log(obj.points[0] + "," + obj.points[1] + "," + obj.width + "," + obj.height);
		context.clearRect(obj.points[0], obj.points[1], obj.width, obj.height);
	}
});

socket.on('drawRec', function(obj){
	context.beginPath();
	var points = obj.points;
    context.rect(points[0], points[1], points[2], points[3]);
    context.closePath();
    
    context.lineWidth = obj.line;
    context.strokeStyle = obj.style;
    context.stroke();
});

socket.on('roomCount', function(obj){
	roomOccupantCountUpdateListener(obj.room, obj.num);
	// roomOccupantCountUpdateListener (roomID, numOccupants)
});

socket.on('attrUpdate', function(jobj){
	processClientAttributeUpdate(jobj.client, jobj.attr, jobj.val);
	// processClientAttributeUpdate (clientID, attrName, attrVal)
});

socket.on('clientQuit', function(obj){
	clientRemovedFromRoomListener(obj.room, obj.client);
});

socket.on('moveMsg', function(obj){
	moveMessageListener(obj.client, obj.val);
	//moveMessageListener (fromClientID, coordsString)
});

socket.on('pathMsg', function(obj){
	pathMessageListener(obj.client, obj.val);
	//pathMessageListener (fromClientID, pathString)
});

// Triggered when the connection to Union Server is closed
function closeListener (e) {
  setStatus("Disconnected from Server.");
  // Stop drawing content sent by other users
  clearInterval(processDrawingCommandsIntervalID);
}

// Triggered when this client has joined the server-side drawing room
function joinedRoomListener (roomID) {
  // Periodically execute drawing commands sent by other users
  processDrawingCommandsIntervalID = setInterval(processDrawingCommands, 20);
}

// Triggered when this client is informed that number of users in the 
// server-side drawing room has changed
function roomOccupantCountUpdateListener (roomID, numOccupants) {
  numOccupants = parseInt(numOccupants);
  if (numOccupants == 1) {
    setStatus("Now drawing on your own (no one else is here at the moment)");
  } else if (numOccupants == 2) {
    setStatus("Now drawing with " + (numOccupants-1) + " other person");
  } else {
    setStatus("Now drawing with " + (numOccupants-1) + " other people");
  }
}

//==============================================================================
// HANDLE INCOMING CLIENT ATTRIBUTES
//==============================================================================
// Triggered when Union Server sends a "snapshot" describing the drawing room,
// including a list of users supplied as unnamed arguments after the 
// roomAttributes parameter. For a description of roomSnapshotListener()'s 
// parameters, see "u54" in the UPC specification, 
// at: http://unionplatform.com/specs/upc/. This client receives the room 
// snapshot automatically when it the joins the drawing room.
function roomSnapshotListener (requestID,
                               roomID,
                               occupantCount,
                               observerCount,
                               roomAttributes) {
  // The unnamed arguments following 'roomAttributes' is a list of 
  // clients in the room. Assign that list to clientList. 
  var clientList = Array.prototype.slice.call(arguments).slice(5);
  var clientID;
  var roomAttrString;
  var roomAttrs;
  var attrName;
  var attrVal;
  
  // Loop through the list of clients in the room to get each client's
  // "thickness" and "color" attributes.
  for (var i = 0; i < clientList.length; i+=5) {
    clientID = clientList[i];
    // Each client's room-scoped client attributes are passed as a 
    // pipe-delimited string. Split that string to get the attributes.
    clientAttrString = clientList[i+4];
    clientAttrs = clientAttrString == "" ? [] : clientAttrString.split("|");
    
    // Pass each client attribute to processClientAttributeUpdate(), which will
    // check for the "thickness" and "color" attributes.
    for (var j = 0; j < clientAttrs.length; j++) {
      attrName = clientAttrs[j];
      attrVal  = clientAttrs[j+1];
      processClientAttributeUpdate(clientID, attrName, attrVal);
    }
  }
}

// Triggered when one of the clients in the drawing room changes an attribute
// value. When an attribute value changes, check to see whether it was either 
// the "thickness" attribute or the "color" attribute.
function clientAttributeUpdateListener (attrScope, 
                                        clientID,
                                        userID,
                                        attrName,
                                        attrVal,
                                        attrOptions) { 
  if (attrScope == roomID) {
    processClientAttributeUpdate(clientID, attrName, attrVal);
  }
}

// Triggered when a clients leaves the drawing room.
function clientRemovedFromRoomListener (roomID, clientID) {
  // The client is gone now, so remove all information pertaining to that client
  delete userThicknesses[clientID];
  delete userColors[clientID];
  delete userCommands[clientID];
  delete userCurrentPositions[clientID];
}

// Checks for changes to the the "thickness" and "color" attributes.
function processClientAttributeUpdate (clientID, attrName, attrVal) {
  if (attrName == Attributes.THICKNESS) {
    // The "thickness" attribute changed, so push a "set thickness" command
    // onto the drawing command stack for the specified client. But first, 
    // bring the thickness into legal range if necessary (prevents thickness hacking).
    addDrawingCommand(clientID, DrawingCommands.SET_THICKNESS, getValidThickness(attrVal));
  } else if (attrName == Attributes.COLOR) {
    // The "color" attribute changed, so push a "set color" command
    // onto the drawing command stack for the specified client
    addDrawingCommand(clientID, DrawingCommands.SET_COLOR, attrVal);
  }
}

//==============================================================================
// HANDLE INCOMING CLIENT MESSAGES
//==============================================================================
// Triggered when a remote client sends a "MOVE" message to this client
function moveMessageListener (fromClientID, coordsString) {
  // Parse the specified (x, y) coordinate
  var coords = coordsString.split(",");
  var position = {x:parseInt(coords[0]), y:parseInt(coords[1])};
  // Push a "moveTo" command onto the drawing-command stack for the sender
  addDrawingCommand(fromClientID, DrawingCommands.MOVE_TO, position);
}

// Triggered when a remote client sends a "PATH" message to this client
function pathMessageListener (fromClientID, pathString) {
  // Parse the specified list of points
  var path = pathString.split(",");
  
  // For each point, push a "lineTo" command onto the drawing-command stack 
  // for the sender
  var position;
  for (var i = 0; i < path.length; i+=2) {
    position = {x:parseInt(path[i]), y:parseInt(path[i+1])};
    addDrawingCommand(fromClientID, DrawingCommands.LINE_TO, position);
  }
}

//==============================================================================
// BROADCAST DRAWING DATA TO OTHER USERS
//==============================================================================
// Sends the local user's drawing-path information to other users in the 
// drawing room.
function broadcastPath () {
  // If there aren't any points buffered (e.g., if the pen is down but not
  // moving), then don't send the PATH message.
  if (bufferedPath.length == 0) {
    return;
  }
  // Use SEND_MESSAGE_TO_ROOMS to deliver the message to all users in the room
  // Parameters are: messageName, roomID, includeSelf, filters, ...args. For
  // details, see http://unionplatform.com/specs/upc/.
  var value = bufferedPath.join(",");
  socket.emit('pathMsg', {room:roomID, client:currentClientId, attr:Messages.PATH, val:value});
  
  // Clear the local user's outgoing path data
  bufferedPath = [];
  // If the user is no longer drawing, stop broadcasting drawing information
  if (!isPenDown) {
    clearInterval(broadcastPathIntervalID);
  }
}

// Sends all users in the drawing room an instruction to reposition the local
// user's pen.
function broadcastMove (x, y) {
	var value = x + "," + y;
	socket.emit('moveMsg', {room:roomID, client:currentClientId, attr:Messages.MOVE, val:value});
}

//==============================================================================
// PROCESS DRAWING COMMANDS FROM OTHER USERS
//==============================================================================
// Pushes a drawing command onto the command stack for the specified client.
// At a regular interval, commands are pulled off the stack and executed,
// causing remote user's drawings to appear on-screen. 
function addDrawingCommand (clientID, commandName, arg) {
  // If this client does not yet have a command stack, make one. 
  if (userCommands[clientID] == undefined) {
    userCommands[clientID] = [];
  }
  // Push the command onto the stack.
  var command = {};
  command["commandName"] = commandName;
  command["arg"] = arg;
  userCommands[clientID].push(command);
}

// Executes the oldest command on all user's command stacks
function processDrawingCommands () {
  var command;
  // Loop over all command stacks
  for (var clientID in userCommands) {
    // Skip empty stacks
    if (userCommands[clientID].length == 0) {
      continue;
    }
    
    // Execute the user's oldest command
    command = userCommands[clientID].shift();
    switch (command.commandName) {
      case DrawingCommands.MOVE_TO:
        userCurrentPositions[clientID] = {x:command.arg.x, y:command.arg.y};
        break;
        
      case DrawingCommands.LINE_TO:
        if (userCurrentPositions[clientID] == undefined) {
          userCurrentPositions[clientID] = {x:command.arg.x, y:command.arg.y};
        } else {
          drawLine(userColors[clientID] || defaultLineColor, 
                   userThicknesses[clientID] || defaultLineThickness, 
                   userCurrentPositions[clientID].x, 
                   userCurrentPositions[clientID].y,
                   command.arg.x, 
                   command.arg.y);
           userCurrentPositions[clientID].x = command.arg.x; 
           userCurrentPositions[clientID].y = command.arg.y; 
        }
        break;
        
      case DrawingCommands.SET_THICKNESS:
        userThicknesses[clientID] = command.arg;
        break;
        
      case DrawingCommands.SET_COLOR:
        userColors[clientID] = command.arg;
        break;
    }
  }
}

//==============================================================================
// TOUCH-INPUT EVENT LISTENERS
//==============================================================================
// On devices that support touch input, this function is triggered when the 
// user touches the screen.
function touchDownListener (e) {
  // Note that this device supports touch so that we can prevent conflicts with
  // mouse input events.
  hasTouch = true;
  // Prevent the touch from scrolling the page, but allow interaction with the
  // control-panel menus. The "event.target.nodeName" variable provides the name
  // of the HTML element that was touched.
  if (event.target.nodeName != "SELECT") {
    e.preventDefault();
  }
  // Determine where the user touched screen.
  var touchX = e.changedTouches[0].clientX - canvas.offsetLeft;
  var touchY = e.changedTouches[0].clientY - canvas.offsetTop;
  // A second "touch start" event may occur if the user touches the screen with
  // two fingers. Ignore the second event if the pen is already down.
  if (!isPenDown) {
    // Move the drawing pen to the position that was touched
    penDown(touchX, touchY);
  }
}

// On devices that support touch input, this function is triggered when the user
// drags a finger across the screen.
function touchMoveListener (e) {
  hasTouch = true;
  e.preventDefault();
  var touchX = e.changedTouches[0].clientX - canvas.offsetLeft;
  var touchY = e.changedTouches[0].clientY - canvas.offsetTop;
  // Draw a line to the position being touched.
  penMove(touchX, touchY);
}

// On devices that support touch input, this function is triggered when the 
// user stops touching the screen.
function touchUpListener () {
  // "Lift" the drawing pen, so lines are no longer drawn
  penUp();
}

//==============================================================================
// MOUSE-INPUT EVENT LISTENERS
//==============================================================================
// Triggered when the mouse is pressed down
function pointerDownListener (e) {
  // If this is an iPhone, iPad, Android, or other touch-capable device, ignore
  // simulated mouse input.
  if (hasTouch) {
    return;
  }
  
  // Retrieve a reference to the Event object for this mousedown event.
  // Internet Explorer uses window.event; other browsers use the event parameter
  var event = e || window.event; 
  // Determine where the user clicked the mouse.
  var mouseX = event.clientX - canvas.offsetLeft;
  var mouseY = event.clientY - canvas.offsetTop;
  
  // Move the drawing pen to the position that was clicked
  penDown(mouseX, mouseY);
  
  // We want mouse input to be used for drawing only, so we need to stop the 
  // browser from/ performing default mouse actions, such as text selection. 
  // In Internet Explorer, we "prevent default actions" by returning false. In 
  // other browsers, we invoke event.preventDefault().
  if (event.preventDefault) {
    if (event.target.nodeName != "SELECT") {
      event.preventDefault();
    }
  } else {
    return false;  // IE
  }
}

// Triggered when the mouse moves
function pointerMoveListener (e) {
  if (hasTouch) {
    return;
  }
  var event = e || window.event; // IE uses window.event, not e
  var mouseX = event.clientX - canvas.offsetLeft;
  var mouseY = event.clientY - canvas.offsetTop;
  
  // Draw a line if the pen is down
  penMove(mouseX, mouseY);

  // Prevent default browser actions, such as text selection
  if (event.preventDefault) {
    event.preventDefault();
  } else {
    return false;  // IE
  }
}

// Triggered when the mouse button is released
function pointerUpListener (e) {
  if (hasTouch) {
    return;
  }
  // "Lift" the drawing pen
  var event = e || window.event; // IE uses window.event, not e
  var mouseX = event.clientX - canvas.offsetLeft;
  var mouseY = event.clientY - canvas.offsetTop;
  
  penUp(mouseX, mouseY);
}

//==============================================================================
// CONTROL PANEL MENU-INPUT EVENT LISTENERS
//==============================================================================
// Triggered when an option in the "line thickness" menu is selected
function thicknessSelectListener (e) {
  // Determine which option was selected
  var newThickness = this.options[this.selectedIndex].value;
  // Locally, set the line thickness to the selected value
  localLineThickness = getValidThickness(newThickness);

  socket.emit('attrUpdate', {room:roomID, client:currentClientId, attr: Attributes.THICKNESS, val:newThickness});
  // After the user selects a value in the drop-down menu, the iPhone
  // automatically scrolls the page, so scroll back to the top-left. 
  iPhoneToTop();
}

// Triggered when an option in the "line color" menu is selected
function colorSelectListener (e) {
	// Determine which option was selected
	var newColor = this.options[this.selectedIndex].value;
	// Locally, set the line color to the selected value
	localLineColor = newColor;
	// Share selected color with other users
	socket.emit('attrUpdate', {room:roomID, client:currentClientId, attr: Attributes.COLOR, val:newColor});
	// Scroll the iPhone back to the top-left. 
	iPhoneToTop();
}

// Triggered when an option in the "line color" menu is selected
function shapeSelectListener (e) {
  // Determine which option was selected
  var newColor = this.options[this.selectedIndex].value;
  if(newColor == "")
	  return;
  isShape = true;
  shapeObj.type = newColor;
  // Locally, set the line color to the selected value
 // localLineColor = newColor;
  // Share selected color with other users
  //socket.emit('attrUpdate', {room:roomID, client:currentClientId, attr: Attributes.COLOR, val:newColor});
  // Scroll the iPhone back to the top-left. 
  //iPhoneToTop();
}

//==============================================================================
// PEN
//==============================================================================
// Places the pen in the specified location without drawing a line. If the pen
// subsequently moves, a line will be drawn.

function penDown (x, y) {
	if(isErase){
		isPenDown = true;
		return;
	}
	
  if(isShape){
	  shapeObj.x = x;
	  shapeObj.y = y;
	  return;
  }
  
  isPenDown = true;
  localPen.x = x;
  localPen.y = y;
  
  // Send this user's new pen position to other users.
  broadcastMove(x, y);
  
  // Begin sending this user's drawing path to other users every 500 milliseconds.//ori is 500
  broadcastPathIntervalID = setInterval(broadcastPath, 50);
}

// Draws a line if the pen is down.
function penMove (x, y) {
	if(isErase && isPenDown){
		socket.emit('clearCanvas', {room:roomID, client:currentClientId, points:[x,y], width:5, height:5});
		context.clearRect(x, y, 5, 5);
		return;
	}
	
	if(isShape){
//            var wid = this.canvas.width;
//            var hei = this.canvas.height;
//	        this.context.clearRect(0, 0, wid, hei);
//	        this.context.drawImage(canvas, 0, 0);
//		
//		this.context.beginPath();
//        this.context.rect(shapeObj.x, shapeObj.y, x-shapeObj.x, y-shapeObj.y);
//        this.context.closePath();
//        
//        context.lineWidth = localLineThickness;
//        context.strokeStyle = localLineColor;
//        this.context.stroke();
		
		return;
	}
	
  if (isPenDown) {
    // Buffer the new position for broadcast to other users. Buffer a maximum
    // of 100 points per second.
    if ((new Date().getTime() - lastBufferTime) >= 0) {
      bufferedPath.push(x + "," + y);
      lastBufferTime = new Date().getTime();
    }
    
    // Draw the line locally.
    drawLine(localLineColor, localLineThickness, localPen.x, localPen.y, x, y);
    
    // Move the pen to the end of the line that was just drawn.
    localPen.x = x;
    localPen.y = y;
  }
}

// "Lifts" the drawing pen, so that lines are no longer draw when the mouse or
// touch-input device moves.
function penUp (x, y) {
	if(isErase && isPenDown){
		isPenDown = false;
		return;
	}
	
	if(isShape){
		if(!shapeObj.x){
			return;
		}
		
		socket.emit('drawRec', {room:roomID, client:currentClientId, line:localLineThickness, style:localLineColor, points:[shapeObj.x, shapeObj.y, x-shapeObj.x, y-shapeObj.y]});
		
		this.context.beginPath();
        this.context.rect(shapeObj.x, shapeObj.y, x-shapeObj.x, y-shapeObj.y);
        this.context.closePath();
        
        context.lineWidth = localLineThickness;
        context.strokeStyle = localLineColor;
        this.context.stroke();
        
        shapeObj.x = 0;
        shapeObj.y = 0;
        
        shapeObj = {};
        isShape = false;
        document.getElementById("shape").selectedIndex = 0;
		
		return;
	}
	
  isPenDown = false;
}

//==============================================================================
// DRAWING
//==============================================================================
// Draws a line on the HTML5 canvas
function drawLine (color, thickness, x1, y1, x2, y2) {
  context.strokeStyle = color;
  context.lineWidth   = thickness;
  
  context.beginPath();
  context.moveTo(x1, y1)
  context.lineTo(x2, y2);
  context.stroke();
}

//==============================================================================
// STATUS
//==============================================================================
// Updates the text of the on-screen HTML "status" div tag
function setStatus (message) {
  document.getElementById("status").innerHTML = message;
}

//==============================================================================
// IPHONE UTILS
//==============================================================================
// Hides the iPhone address bar by scrolling it out of view
function iPhoneToTop () {
  if (navigator.userAgent.indexOf("iPhone") != -1) {
    setTimeout (function () {
      window.scroll(0, 0);
    }, 100);
  }
}

//==============================================================================
// DATA VALIDATION
//==============================================================================
function getValidThickness (value) {
  value = parseInt(value);
  var thickness = isNaN(value) ? defaultLineThickness : value;
  return Math.max(1, Math.min(thickness, maxLineThickness));
}

function clearCanvas(){
	socket.emit('clearCanvas', {room:roomID, client:currentClientId});
	context.clearRect(0, 0, canvas.width, canvas.height);
}

var isErase = false;
function erase(){
	isErase = !isErase;
}