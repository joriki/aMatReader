from pyCyAMatReader import AMatReader, CyRESTInstance, Core, CyFailedCIError

BASE_URL = "http://127.0.0.1"  # at UCSD
IMPOSSIBLE_URL = "http://impossible"

from requests.status_codes import codes

""" Built from http://pyunit.sourceforge.net/pyunit.html """


import unittest

_amatreader = AMatReader(CyRESTInstance(base_url=BASE_URL))  # assumes Cytoscape answers at base_url
_bad_amatreader = AMatReader(CyRESTInstance(base_url=IMPOSSIBLE_URL))  # # for verifying proper exception thrown

_core = Core(CyRESTInstance(base_url=BASE_URL))  # assumes Cytoscape answers at base_url


import os
path = os.path.realpath(__file__)
path = os.path.dirname(path)
path = os.path.dirname(path)
SAMPLE_DIR = os.path.join(path, 'samples')
SAMPLE_FILE = os.path.join(SAMPLE_DIR, "sample.mat")
CRAZY_SAMPLE = os.path.join(SAMPLE_DIR, "crazyTest.csv")
CRAZY_SAMPLE2 = os.path.join(SAMPLE_DIR, "crazyTest2.txt")
CRAZY_SAMPLE3 = os.path.join(SAMPLE_DIR, "crazyTest3.txt")


SAMPLE_NO_HEADER_ROW = os.path.join(SAMPLE_DIR, "sampleNoHeaderRow.mat")
SAMPLE_NO_HEADER_COLUMN = os.path.join(SAMPLE_DIR, "sampleNoHeaderColumn.mat")
SAMPLE_NO_HEADERS = os.path.join(SAMPLE_DIR, "sampleNoHeaders.mat")


BASE_DATA = {
  "files": [SAMPLE_FILE],
  "delimiter": "TAB",
  "ignoreZeros": True,
  "undirected": False,
  "interactionName": "interacts with",
  "rowNames": True,
  "columnNames": True
}

class AMatReaderTestCase(unittest.TestCase):
	#_SESSION_FILE = "/git/cytoscape/cytoscape/gui-distribution/assembly/target/cytoscape/sampleData/galFiltered.cys" # this must be modified if your local galFiltered.cys is not in this location.

	def setUp(self):
		pass

	def tearDown(self):
		pass


	def test_amatreader_no_files(self):
		data = BASE_DATA.copy()
		del data['files']
		try:
			_amatreader.import_matrix(data)
		except CyFailedCIError as e:
			error = e.args[0]
			assert error["type"] == "urn:cytoscape:ci:aMatReader-app:v1:aMatReader:2" \
				   and error["status"] == 400 \
				   and error["message"] is not None \
				   and error["link"] is not None, "test_amatreader_no_network returned invalid CyFailedError: " + str(e)
		else:
			assert False, "test_amatreader_no_network did not get the expected CyFailedError exception"

	def test_amatreader_nonexistent_file(self):
		data = BASE_DATA.copy()
		data["files"] = ["NONEXISTENT"]
		try:
			_amatreader.import_matrix(data)
		except CyFailedCIError as e:
			error = e.args[0]
			assert error["type"] == "urn:cytoscape:ci:aMatReader-app:v1:aMatReader:1" \
				   and error["status"] == codes.NOT_FOUND \
				   and error["message"] is not None \
				   and error["link"] is not None, "test_amatreader_no_network returned invalid CyFailedError: " + str(e)
		else:
			assert False, "test_amatreader_no_network did not get the expected CyFailedError exception"

	def test_amatreader_one_nonexistent_file(self):
		data = BASE_DATA.copy()
		data["files"] = [SAMPLE_FILE, "NONEXISTENT"]

		try:
			_amatreader.import_matrix(data)
		except CyFailedCIError as e:
			error = e.args[0]
			assert error["type"] == "urn:cytoscape:ci:aMatReader-app:v1:aMatReader:1" \
				   and error["status"] == codes.NOT_FOUND \
				   and error["message"] is not None \
				   and error["link"] is not None, "test_amatreader_no_network returned invalid CyFailedError: " + str(e)
		else:
			assert False, "test_amatreader_no_network did not get the expected CyFailedError exception"


	def test_amatreader(self):
		data = BASE_DATA.copy()

		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
		assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

		_amatreader.remove_network(result['suid'])

	def test_amatreader_crazy(self):
		data = BASE_DATA.copy()

		data.update({'files': [CRAZY_SAMPLE],
			'delimiter': 'COMMA',
			'removeColumnPrefix': True
		})
		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
		assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

		_amatreader.remove_network(result['suid'])

	def test_amatreader_crazy2(self):
		data = BASE_DATA.copy()
		data.update({'files': [CRAZY_SAMPLE2],
			'delimiter': 'SPACE',
			'columnNames': False,
		})
		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
		assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

		_amatreader.remove_network(result['suid'])

	def test_amatreader_crazy3(self):
		data = BASE_DATA.copy()
		data.update({'files': [CRAZY_SAMPLE3],
			'delimiter': 'BAR',
			'rowNames': False,
		})
		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
		assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

		_amatreader.remove_network(result['suid'])

	def test_amatreader_2(self):
		data = BASE_DATA.copy()
		data["files"] = [SAMPLE_FILE, SAMPLE_FILE]

		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, 'newEdges value should be 10, %s' % result
		assert result['updatedEdges'] == 10, 'updatedEdges value should be 0, %s' % result
		_amatreader.remove_network(result['suid'])

	def test_amatreader_extend(self):
		AMATREADER_COLUMN = "sample.mat"
		data = BASE_DATA.copy()

		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
		assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

		suid = result['suid']
		result = _amatreader.import_matrix(data, suid=suid)
		assert result['newEdges'] == 0, 'newEdges value should be 0, not %s' % result
		assert result['updatedEdges'] == 10, 'updatedEdges value should be 10, not %s' % result

		_amatreader.remove_network(result['suid'])

	def test_amatreader_extend_2(self):
		AMATREADER_COLUMN = "sample.mat"
		data = BASE_DATA.copy()

		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
		assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

		suid = result['suid']
		data['files'] = [SAMPLE_FILE, SAMPLE_FILE, SAMPLE_FILE]
		result = _amatreader.import_matrix(data, suid=suid)
		assert result['newEdges'] == 0, 'newEdges value should be 0, not %s' % result
		assert result['updatedEdges'] == 30, 'updatedEdges value should be 10, not %s' % result

		_amatreader.remove_network(result['suid'])

	def test_amatreader_bad_delimiter(self):
		data = BASE_DATA.copy()
		data['delimiter'] = 'ERROR'

		try:
			_amatreader.import_matrix(data)
		except CyFailedCIError as e:
			error = e.args[0]
			assert error["type"] == "urn:cytoscape:ci:aMatReader-app:v1:aMatReader:2" \
				   and error["status"] == 500 \
				   and error["message"] is not None \
				   and error["link"] is not None, "test_amatreader_no_network returned invalid CyFailedError: " + str(e)
		else:
			assert False, "test_amatreader_no_network did not get the expected CyFailedError exception"

	def test_amatreader_bad_format(self):
		data = BASE_DATA.copy()
		data['delimiter'] = 'ERROR'

		try:
			_amatreader.import_matrix(data)
		except CyFailedCIError as e:
			error = e.args[0]
			assert error["type"] == "urn:cytoscape:ci:aMatReader-app:v1:aMatReader:2" \
				   and error["status"] == 500 \
				   and error["message"] is not None \
				   and error["link"] is not None, "test_amatreader_no_network returned invalid CyFailedError: " + str(e)
		else:
			assert False, "test_amatreader_no_network did not get the expected CyFailedError exception"


	def test_amatreader_no_column_names(self):
		data = BASE_DATA.copy()
		data.update({ "files": [SAMPLE_NO_HEADER_ROW], 'columnNames': False})

		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
		assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

		_amatreader.remove_network(result['suid'])


	def test_amatreader_no_row_names(self):
		data = BASE_DATA.copy()
		data.update({'files': [SAMPLE_NO_HEADER_COLUMN], 'rowNames': False})
		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
		assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

		_amatreader.remove_network(result['suid'])

	def test_amatreader_undirected(self):
		data = BASE_DATA.copy()
		data.update({"files": [SAMPLE_FILE], "undirected": True})
		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 5, 'newEdges value should be 5, not %s' % result
		assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

		_amatreader.remove_network(result['suid'])

	def test_amatreader_no_headers(self):
		data = BASE_DATA.copy()
		data.update({'files': [SAMPLE_NO_HEADERS],
		'rowNames': False,
		'columnNames': False,
		'delimiter': 'BAR',
		})
		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
		assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

		_amatreader.remove_network(result['suid'])

	def test_amatreader_exception(self):
		try:
			_bad_amatreader.import_matrix()
		except CyFailedCIError:
			assert False, "test_amatreader_exception got unexpected CyFailedError"
		except BaseException:
			pass
		else:
			assert False, "test_amatreader_exception expected exception"

	def test_amatreader_column_prefix(self):
		data = BASE_DATA.copy()
		data['files'] = [os.path.join(SAMPLE_DIR, 'sampleColumnPrefixes.mat')]
		data['removeColumnPrefix'] = True
		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 9, "Should be 9 new edges from file"
		_amatreader.remove_network(result['suid'])

	def test_amatreader_column_prefix_with_zeros(self):
		data = BASE_DATA.copy()
		data['files'] = [os.path.join(SAMPLE_DIR, 'sampleColumnPrefixes.mat')]
		data['removeColumnPrefix'] = True
		data['ignoreZeros'] = False
		result = _amatreader.import_matrix(data)
		assert result['newEdges'] == 10, "Should be 10 new edges from file"
		_amatreader.remove_network(result['suid'])


	def import_numpy(self):
		_amatreader = AMatReader()  # assumes Cytoscape answers at base_url
		mat = np.random.random([5, 5])
		data = BASE_DATA.copy()
		data['columnNames'] = False
		data['rowNames'] = False
		data['undirected'] = 'true'
		result = _amatreader.import_numpy(mat, data, names=['A', 'B', 'C', 'D', 'E'])
		assert result['newEdges'] == 10, "Should be 10 new edges not " + result['newEdges']
		_amatreader.remove_network(result['suid'])

	def import_pandas(self):
		import pandas as pd
		_amatreader = AMatReader(CyRESTInstance(base_url=BASE_URL))  # assumes Cytoscape answers at base_url
		data = BASE_DATA.copy()
		data['rowNames'] = False
		df = pd.DataFrame(np.random.randint(low=0, high=10, size=(5, 5)),
			columns=['a', 'b', 'c', 'd', 'e'])
		result = _amatreader.import_pandas(df, data)
		assert result['newEdges'] == 25, "Should be 25 newEdges not " + result['newEdges']
		_amatreader.remove_network(result['suid'])


def suite():
	amatreader_suite = unittest.makeSuite(AMatReaderTestCase, "test")
	return unittest.TestSuite((amatreader_suite))


if __name__ == "__main__":
	unittest.TextTestRunner().run(suite())
