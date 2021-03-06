var Diagnostics = require('../models/diagnostics');

exports.saveDiagnosticsData = function(req, res) {

	console.log(req.body);

	var diagnosticsData = new Diagnostics({

		batteryLevel: req.body.batteryLevel,
		timeStamp: Date.now()

	});

	console.log("diagnosticsData: " + diagnosticsData);

	diagnosticsData.save(function(err) {
		if (err) {
			console.log("Error saving diagnosticsData");
			res.send(404, "Error saving diagnosticsData");
		}

		console.log("Success saving diagnosticsData");

		res.send(200, req.body.batteryLevel);
	});
};

exports.getLatestDiagnosticsData = function(req, res) {

	Diagnostics.find({}, null, {sort: {'_id': -1}}, function(err, docs) {
		console.log("diagnosticsData");

		console.log(docs.length);

	 	if (docs.length == 0) {
	 		res.status(200).json("NO DATA");
	 	}
	 	else {
	 		res.status(200).json(docs[0]);
	 	}
	});

};