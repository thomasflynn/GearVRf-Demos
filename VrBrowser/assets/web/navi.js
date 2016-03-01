/* NAVI JS client library */
'use strict';

// better way to do mock interface?
// maybe with js Proxy+Decorators when support arrives
var _NAVI = _NAVI || {
  showToast: function(){},

  launchWebVR: function(url){ console.log('_NAVI.launchWebVR: ' + url); },

  getValue: function(){},
  setValue: function(){},

  getBackground: function(){},
  setBackground: function(bg){ console.log('_NAVI:setBackground: ' + bg); },

  createObject: function(){},
  addObjectToScene: function(){},
  removeObjectFromScene: function(){},
  translateObject: function(){},
  rotateObject: function(){},
  setObjectPosition: function(){},
  setObjectRotation: function(){},
  setObjectScale: function(){},
  setObjectVisible: function(){},
  visibleObject: function(){}
};

var NAVI = {

  version: '0.1',

  launchWebVR: function(url) {
    return _NAVI.launchWebVR(url);
  },

  showToast: function(text) {
    return _NAVI.showToast(text);
  },

  getValue: function(key) {
    return _NAVI.getValue(key);
  },

  setValue: function(key, value) {
    return _NAVI.setValue(key, value);
  },


  /* background */
  getBackground: function() {
    return _NAVI.getBackground();
  },

  setBackground: function(bg) {
    // rgb => hex : rgb(255,255,255) => #ffffff
    if (/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/i.test( bg )) {
      bg = NAVI.util.rgb2hex( bg );
    }
    return _NAVI.setBackground(bg);
  }

};

/* Scene */
NAVI.Scene = function(name) {
  this.name = name || '';
  this.id = NAVI.Scene._id++;

  this.children = [];
};

NAVI.Scene._id = 0;

NAVI.Scene.prototype.add = function(object) {
  _NAVI.addObjectToScene(this.name, object.name);
};

NAVI.Scene.prototype.remove = function(object) {
  _NAVI.removeObjectFromScene(this.name, object.name);
};

/* Object3D */
NAVI.Object3D = function(name, type) {
  this.name = name;
  this.type = type;

  this.id = NAVI.Object3D._id++;
  //this.uuid = _NAVI.getUUID();

  this.parent = false;
  this.children = [];

  this._position = [0,0,0];
  this._rotation = [0,0,0,0];
  this._scale = [1,1,1];

  this._visible = true;

  this._native_id = null;

  this._init();
};

Object.defineProperties(NAVI.Object3D.prototype, {
  'position': {
    get: function() {
      return this._position;
    },
    set: function(val) {
      if (val.length !== 3)
        return;

      this._position = val;

      _NAVI.setObjectPosition(this.name, val[0], val[1], val[2]);
    }
  },
  'rotation': {
    get: function() {
      return this._rotation;
    },
    set: function(val) {
      if (val.length !== 4)
        return;

      this._rotation = val;

      _NAVI.setObjectRotation(this.name, val[0], val[1], val[2], val[3]);
    }
  },
  'scale': {
    get: function() {
      return this._scale;
    },
    set: function(val) {
      if (val.length != 3)
        return;

      this._scale = val;

      _NAVI.setObjectScale(this.name, val[0], val[1], val[2]);
    }
  },
  'visible': {
    get: function() {
      return this._visible;
    },
    set: function(val) {
      if (val === this._visible)
        return;

      this._visible = val;

      _NAVI.setObjectVisible(this.name, this._visible);
    }
  }
});

NAVI.Object3D._id = 0;

NAVI.Object3D.prototype._init = function() {
  _NAVI.createObject(this.name, this.type);
};

NAVI.Object3D.prototype.setPosition = function(x,y,z) {
  this.position = [x,y,z];
};

NAVI.Object3D.prototype.translate = function(x,y,z) {
  _NAVI.translateObject(this.name, x,y,z);
};

NAVI.Object3D.prototype.setRotation = function(w,x,y,z) {
  this.rotation = [w,x,y,z];
};

// rotate angle around axis:[x,y,z]
NAVI.Object3D.prototype.rotate = function(angle, x,y,z) {
  _NAVI.rotateObject(this.name, angle, x,y,z);
};

NAVI.Object3D.prototype.setScale = function(x,y,z) {
  this.scale = [x,y,z];
};

NAVI.Object3D.prototype.destroy = function() {
  //_NAVI.destroyObject(this.name);
};

NAVI.Object3D.prototype.show = function() {
  _NAVI.visibleObject(this.name, true);
};

NAVI.Object3D.prototype.hide = function() {
  _NAVI.visibleObject(this.name, false);
};


/* Object types */
NAVI.Plane = function(name) {
  NAVI.Object3D.call(this, name, 'plane');
};
NAVI.Plane.prototype = Object.create( NAVI.Object3D.prototype );

NAVI.Box = function(name) {
  NAVI.Object3D.call(this, name, 'cube');
};
NAVI.Box.prototype = Object.create( NAVI.Object3D.prototype );

NAVI.Sphere = function(name) {
  NAVI.Object3D.call(this, name, 'sphere');
};
NAVI.Sphere.prototype = Object.create( NAVI.Object3D.prototype );

NAVI.Cylinder = function(name) {
  NAVI.Object3D.call(this, name, 'cylinder');
};
NAVI.Cylinder.prototype = Object.create( NAVI.Object3D.prototype );


/* util */
NAVI.util = {
  isColorString: function(string) {
    // rgb(255,0,0) | rgb(100%,0%,0%) | #ff0000
    return /^rgb\((\d+), ?(\d+), ?(\d+)\)$/i.test( string )
      || /^rgb\((\d+)\%, ?(\d+)\%, ?(\d+)\%\)$/i.test( string )
      || /^\#([0-9a-f]{6})$/i.test( string );
  },

  // currently only allows gradients defined with hex i.e. "#ff0000,#339966,#9933ff"
  isGradientString: function(string) {
    return (string.indexOf(',') !== -1) && string.indexOf('rgb') !== 0;
  },


  rgb2hex: function(rgb) {
    var rgb = rgb.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/);
    function hex(x) {
      return ("0" + parseInt(x).toString(16)).slice(-2);
    }
    return "#" + hex(rgb[1]) + hex(rgb[2]) + hex(rgb[3]);
  },

  isUrl: function(string) {
    return string.indexOf('http') === 0;
  }
};

/* Page context processing */
NAVI.process = function() {
  NAVI.processMeta();
};

NAVI.processMeta = function() {
  var metas = document.getElementsByTagName('meta');

  for (i = 0; i < metas.length; i++) {
    var meta = metas[i];
    var name = meta.getAttribute('name');

    if (name === 'vr-background') {
      var content = meta.getAttribute('content');
      NAVI.setBackground( content );
    }
  }
};

// TODO: invoke on page load event
setTimeout(function(){
  NAVI.process();
}, 3000);


