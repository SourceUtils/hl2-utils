function time() {
    var d = new Date();
    var n = d.getTime();
    println('say Unix time: ' + n);
}

function reset(cvar) {
    println(cvar + ' ""');
}

function echo(line) {
    println('echo ' + line);
}