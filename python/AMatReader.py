import json, requests
import tempfile
from CyCaller import CyCaller
from CyRESTInstance import CyRESTInstance
from TestConfig import BASE_URL, IMPOSSIBLE_URL
import numpy as np

BASE_DATA = {
  "delimiter": "TAB",
  "ignoreZeros": True,
  "symmetry": "ASYMMETRIC",
  "interactionName": "interacts with",
  "rowNames": True,
  "columnNames": True
}

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

	def import_numpy(self, matrix, data):
		n, path = tempfile.mkstemp()
		np.savetxt(path, matrix, delimiter='\t', fmt='%g')
		data['files'] = [path]
		return self._cy_caller.execute_post("/aMatReader/v1/import", json.dumps(data))


	def import_pandas(self, df, data):
		n, path = tempfile.mkstemp()
		df.to_csv(path, sep='\t', index=False)
		data['files'] = [path]
		return self._cy_caller.execute_post("/aMatReader/v1/import", json.dumps(data))


	def remove_network(self, suid):
		""" Remove a network from Cytoscape """
		#return self._cy_caller._execute("DELETE", "/v1/networks/" + str(suid))
		return requests.request("DELETE",
								  self._cy_caller.cy_rest_instance.base_url + ":" + str(self._cy_caller.cy_rest_instance.port) + "/v1/networks/" + str(suid))

def save_numpy():
	_amatreader = AMatReader(CyRESTInstance(base_url=BASE_URL))  # assumes Cytoscape answers at base_url
	mat = np.random.random([5, 5])
	data = BASE_DATA.copy()
	data['columnNames'] = False
	data['rowNames'] = False
	data['symmetry'] = 'SYMMETRIC_TOP'
	_amatreader.import_numpy(mat, data)

def save_pandas():
	import pandas as pd
	_amatreader = AMatReader(CyRESTInstance(base_url=BASE_URL))  # assumes Cytoscape answers at base_url
	data = BASE_DATA.copy()
	data['rowNames'] = False
	df = pd.DataFrame(np.random.randint(low=0, high=10, size=(5, 5)),
		columns=['a', 'b', 'c', 'd', 'e'])
	_amatreader.import_pandas(df, data)

if __name__ == '__main__':
	save_pandas()
