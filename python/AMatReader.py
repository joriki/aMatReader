import json, requests

from CyCaller import CyCaller


class AMatReader:
	""" Cover functions for AMatReader functions """

	def __init__(self, cy_rest_instance=None):
		""" Constructor remembers CyREST location """
		self._cy_caller = CyCaller(cy_rest_instance)

	def import_matrix(self, data):
		""" Execute a simple diffusion on current network """
		return self._cy_caller.execute_post("/aMatReader/v1/import", json.dumps(data))

	def extend_matrix(self, suid, data):
		""" Execute a diffusion with options on current network """
		url = "/aMatReader/v1/extend/" + str(suid)
		return self._cy_caller.execute_post(url, json.dumps(data))

	def remove_network(self, suid):
		""" Remove a network from Cytoscape """
		#return self._cy_caller._execute("DELETE", "/v1/networks/" + str(suid))
		return requests.request("DELETE",
								  self._cy_caller.cy_rest_instance.base_url + ":" + str(self._cy_caller.cy_rest_instance.port) + "/v1/networks/" + str(suid))
