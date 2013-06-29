function time() {
    var d = new Date();
    var n = d.getTime();
    println('say Current Unix time: ' + n);
}

function reset(cvar) {
    println(cvar + ' ""');
}

function echo(line) {
    println('echo ' + line);
}

function up() {
    println('+jump');
}

function down() {
    println('-jump');
}

var store = 'sdf';

function setStore(s) {
    store = s;
}

function getStore() {
    println(store);
}

function xhair() {
    for (var i = 1; i < 5; i++) {
        println('cl_crosshair_file crosshair' + i);
    }
}