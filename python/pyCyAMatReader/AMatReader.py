import json, requests
import tempfile
from .CyCaller import CyCaller
from .CyRESTInstance import CyRESTInstance
import numpy as np

class AMatReader:
	""" Cover functions for AMatReader functions """

	def __init__(self, cy_rest_instance=None):
		""" Constructor remembers CyREST location """
		self._cy_caller = CyCaller(cy_rest_instance)

	def import_matrix(self, data, suid=None):
		""" Import adjacency matrix file into Cytoscape """
		if suid is None:
			return self._cy_caller.execute_post("/aMatReader/v1/import", json.dumps(data))
		else:
			return self._cy_caller.execute_post("/aMatReader/v1/extend/" + str(suid), json.dumps(data))

	def import_numpy(self, matrix, data, suid=None, names=[]):
		""" Save matrix to temporary file and import into Cytoscape as adjacency matrix """
		n, path = tempfile.mkstemp()
		args = {'delimiter':'\t', 'fmt':'%g'}
		data['delimiter'] = 'TAB'
		if names:
			args['header'] = '\t'.join(names)
			args['comments']='' # don't comment out the header line
			data['columnNames'] = True
		np.savetxt(path, matrix, **args)
		data['files'] = [path]
		return self.import_matrix(data, suid=suid)


	def import_pandas(self, df, data, suid=None):
		""" Save dataframe to temporary file and import into Cytoscape as adjacency matrix """
		n, path = tempfile.mkstemp()
		df.to_csv(path, sep='\t', index=False)
		data['files'] = [path]
		return self.import_matrix(data, suid=suid)


	def remove_network(self, suid):
		""" Remove a network from Cytoscape """
		#return self._cy_caller._execute("DELETE", "/v1/networks/" + str(suid))
		return requests.request("DELETE",
								  self._cy_caller.cy_rest_instance.base_url + ":" + str(self._cy_caller.cy_rest_instance.port) + "/v1/networks/" + str(suid))
