function execOnNodes(dao) {
    var iter = dao.startFromBeginningIterator();
    var record, key;

    var arr = [];

    while (iter.hasNext()) {
        record = iter.next();
        key = record.getKeyAsNumber();
        arr.push(key);
    }

    arr.sort(function (a, b) {
        return a - b;
    });

    var tenBigNumber = [];

    for (var i = 9; i >= 0; i--) {
        tenBigNumber[i] = arr.pop();
    }

    return String(tenBigNumber);
}

function execOnCoordinator(results) {
    var arr = [];
    for (var i = 0; i < results.size(); i++) {
        arr[i] = parseInt(results.get(i));
    }

    arr.sort(function (a, b) {
        return a - b;
    });

    var tenBigNumber = [];

    for (var i = 9; i >= 0; i--) {
        tenBigNumber[i] = arr.pop();
    }

    return String(tenBigNumber);
}