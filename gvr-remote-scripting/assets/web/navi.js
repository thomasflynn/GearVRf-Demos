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


// CSS colors
// https://developer.mozilla.org/en-US/docs/Web/CSS/color_value
NAVI.ColorKeywords = {
  'aliceblue': 0xF0F8FF,
  'antiquewhite': 0xFAEBD7,
  'aqua': 0x00FFFF,
  'aquamarine': 0x7FFFD4,
  'azure': 0xF0FFFF,
  'beige': 0xF5F5DC,
  'bisque': 0xFFE4C4,
  'black': 0x000000,
  'blanchedalmond': 0xFFEBCD,
  'blue': 0x0000FF,
  'blueviolet': 0x8A2BE2,
  'brown': 0xA52A2A,
  'burlywood': 0xDEB887,
  'cadetblue': 0x5F9EA0,
  'chartreuse': 0x7FFF00,
  'chocolate': 0xD2691E,
  'coral': 0xFF7F50,
  'cornflowerblue': 0x6495ED,
  'cornsilk': 0xFFF8DC,
  'crimson': 0xDC143C,
  'cyan': 0x00FFFF,
  'darkblue': 0x00008B,
  'darkcyan': 0x008B8B,
  'darkgoldenrod': 0xB8860B,
  'darkgray': 0xA9A9A9,
  'darkgreen': 0x006400,
  'darkgrey': 0xA9A9A9,
  'darkkhaki': 0xBDB76B,
  'darkmagenta': 0x8B008B,
  'darkolivegreen': 0x556B2F,
  'darkorange': 0xFF8C00,
  'darkorchid': 0x9932CC,
  'darkred': 0x8B0000,
  'darksalmon': 0xE9967A,
  'darkseagreen': 0x8FBC8F,
  'darkslateblue': 0x483D8B,
  'darkslategray': 0x2F4F4F,
  'darkslategrey': 0x2F4F4F,
  'darkturquoise': 0x00CED1,
  'darkviolet': 0x9400D3,
  'deeppink': 0xFF1493,
  'deepskyblue': 0x00BFFF,
  'dimgray': 0x696969,
  'dimgrey': 0x696969,
  'dodgerblue': 0x1E90FF,
  'firebrick': 0xB22222,
  'floralwhite': 0xFFFAF0,
  'forestgreen': 0x228B22,
  'fuchsia': 0xFF00FF,
  'gainsboro': 0xDCDCDC,
  'ghostwhite': 0xF8F8FF,
  'gold': 0xFFD700,
  'goldenrod': 0xDAA520,
  'gray': 0x808080,
  'green': 0x008000,
  'greenyellow': 0xADFF2F,
  'grey': 0x808080,
  'honeydew': 0xF0FFF0,
  'hotpink': 0xFF69B4,
  'indianred': 0xCD5C5C,
  'indigo': 0x4B0082,
  'ivory': 0xFFFFF0,
  'khaki': 0xF0E68C,
  'lavender': 0xE6E6FA,
  'lavenderblush': 0xFFF0F5,
  'lawngreen': 0x7CFC00,
  'lemonchiffon': 0xFFFACD,
  'lightblue': 0xADD8E6,
  'lightcoral': 0xF08080,
  'lightcyan': 0xE0FFFF,
  'lightgoldenrodyellow': 0xFAFAD2,
  'lightgray': 0xD3D3D3,
  'lightgreen': 0x90EE90,
  'lightgrey': 0xD3D3D3,
  'lightpink': 0xFFB6C1,
  'lightsalmon': 0xFFA07A,
  'lightseagreen': 0x20B2AA,
  'lightskyblue': 0x87CEFA,
  'lightslategray': 0x778899,
  'lightslategrey': 0x778899,
  'lightsteelblue': 0xB0C4DE,
  'lightyellow': 0xFFFFE0,
  'lime': 0x00FF00,
  'limegreen': 0x32CD32,
  'linen': 0xFAF0E6,
  'magenta': 0xFF00FF,
  'maroon': 0x800000,
  'mediumaquamarine': 0x66CDAA,
  'mediumblue': 0x0000CD,
  'mediumorchid': 0xBA55D3,
  'mediumpurple': 0x9370DB,
  'mediumseagreen': 0x3CB371,
  'mediumslateblue': 0x7B68EE,
  'mediumspringgreen': 0x00FA9A,
  'mediumturquoise': 0x48D1CC,
  'mediumvioletred': 0xC71585,
  'midnightblue': 0x191970,
  'mintcream': 0xF5FFFA,
  'mistyrose': 0xFFE4E1,
  'moccasin': 0xFFE4B5,
  'navajowhite': 0xFFDEAD,
  'navy': 0x000080,
  'oldlace': 0xFDF5E6,
  'olive': 0x808000,
  'olivedrab': 0x6B8E23,
  'orange': 0xFFA500,
  'orangered': 0xFF4500,
  'orchid': 0xDA70D6,
  'palegoldenrod': 0xEEE8AA,
  'palegreen': 0x98FB98,
  'paleturquoise': 0xAFEEEE,
  'palevioletred': 0xDB7093,
  'papayawhip': 0xFFEFD5,
  'peachpuff': 0xFFDAB9,
  'peru': 0xCD853F,
  'pink': 0xFFC0CB,
  'plum': 0xDDA0DD,
  'powderblue': 0xB0E0E6,
  'purple': 0x800080,
  'rebeccapurple': 0x663399,
  'red': 0xFF0000,
  'rosybrown': 0xBC8F8F,
  'royalblue': 0x4169E1,
  'saddlebrown': 0x8B4513,
  'salmon': 0xFA8072,
  'sandybrown': 0xF4A460,
  'seagreen': 0x2E8B57,
  'seashell': 0xFFF5EE,
  'sienna': 0xA0522D,
  'silver': 0xC0C0C0,
  'skyblue': 0x87CEEB,
  'slateblue': 0x6A5ACD,
  'slategray': 0x708090,
  'slategrey': 0x708090,
  'snow': 0xFFFAFA,
  'springgreen': 0x00FF7F,
  'steelblue': 0x4682B4,
  'tan': 0xD2B48C,
  'teal': 0x008080,
  'thistle': 0xD8BFD8,
  'tomato': 0xFF6347,
  'turquoise': 0x40E0D0,
  'violet': 0xEE82EE,
  'wheat': 0xF5DEB3,
  'white': 0xFFFFFF,
  'whitesmoke': 0xF5F5F5,
  'yellow': 0xFFFF00,
  'yellowgreen': 0x9ACD32
};
