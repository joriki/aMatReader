from AMatReader import AMatReader
from CyRESTInstance import CyRESTInstance
from TestConfig import BASE_URL, IMPOSSIBLE_URL
from Core import Core
from CyFailedCIError import CyFailedCIError
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
path = os.path.join(path, 'samples', 'sample.mat')

BASE_DATA = {
  "delimiter": "TAB",
  "undirected": False,
  "interactionName": "interacts with",
  "headerColumn": "NAMES",
  "headerRow": "NAMES"
}

class AMatReaderTestCase(unittest.TestCase):
    #_SESSION_FILE = "/git/cytoscape/cytoscape/gui-distribution/assembly/target/cytoscape/sampleData/galFiltered.cys" # this must be modified if your local galFiltered.cys is not in this location.

    def setUp(self):
        pass

    def tearDown(self):
        pass
        """
    def test_amatreader_no_files(self):
        data = BASE_DATA.copy()

        try:
            _amatreader.import_matrix(data)
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == "urn:cytoscape:ci:aMatReader-app:v1:aMatReader:2" \
                   and error["status"] == 400 \
                   and error["message"] is not None \
                   and error["link"] is not None, "test_amatreader_no_network returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_amatreader_no_network did not get the expected CyFailedError exception"

    def test_amatreader_nonexistant_file(self):
        data = BASE_DATA.copy()
        data.update({"files": ["NONEXISTANT"]})
        try:
            _amatreader.import_matrix(data)
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == "urn:cytoscape:ci:aMatReader-app:v1:aMatReader:1" \
                   and error["status"] == codes.NOT_FOUND \
                   and error["message"] is not None \
                   and error["link"] is not None, "test_amatreader_no_network returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_amatreader_no_network did not get the expected CyFailedError exception"

    def test_amatreader_one_nonexistant_file(self):
        data = BASE_DATA.copy()
        data.update({"files": [path, "NONEXISTANT"]})

        try:
            _amatreader.import_matrix(data)
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == "urn:cytoscape:ci:aMatReader-app:v1:aMatReader:1" \
                   and error["status"] == codes.NOT_FOUND \
                   and error["message"] is not None \
                   and error["link"] is not None, "test_amatreader_no_network returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_amatreader_no_network did not get the expected CyFailedError exception"


    def test_amatreader(self):
        data = BASE_DATA.copy()
        data.update({"files": [path]})
        result = _amatreader.import_matrix(data)
        assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
        assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

        _amatreader.remove_network(result['suid'])

    def test_amatreader_2(self):
        data = BASE_DATA.copy()
        data.update({ "files": [path, path]})

        result = _amatreader.import_matrix(data)
        assert result['newEdges'] == 10, 'newEdges value should be 10, %s' % result
        assert result['updatedEdges'] == 10, 'updatedEdges value should be 0, %s' % result
        _amatreader.remove_network(result['suid'])

    def test_amatreader_extend(self):
        AMATREADER_COLUMN = "sample.mat"
        data = BASE_DATA.copy()
        data.update({ "files": [path]})

        result = _amatreader.import_matrix(data)
        assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
        assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

        suid = result['suid']
        result = _amatreader.extend_matrix(suid, data)
        assert result['newEdges'] == 0, 'newEdges value should be 0, not %s' % result
        assert result['updatedEdges'] == 10, 'updatedEdges value should be 10, not %s' % result

        _amatreader.remove_network(result['suid'])

    def test_amatreader_extend_2(self):
        AMATREADER_COLUMN = "sample.mat"
        data = BASE_DATA.copy()
        data.update({ "files": [path]})
        result = _amatreader.import_matrix(data)
        assert result['newEdges'] == 10, 'newEdges value should be 10, not %s' % result
        assert result['updatedEdges'] == 0, 'updatedEdges value should be 0, not %s' % result

        suid = result['suid']
        data['files'] = [path, path, path]
        result = _amatreader.extend_matrix(suid, data)
        assert result['newEdges'] == 0, 'newEdges value should be 0, not %s' % result
        assert result['updatedEdges'] == 30, 'updatedEdges value should be 10, not %s' % result

        _amatreader.remove_network(result['suid'])
        """
    def test_amatreader_bad_delimiter(self):
        data = BASE_DATA.copy()
        data['files'] = [path]
        data['delimiter'] = 'ERROR'

        try:
            _amatreader.import_matrix(data)
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == "urn:cytoscape:ci:aMatReader-app:v1:aMatReader:3" \
                   and error["status"] == 400 \
                   #and error["message"] is not None \
                   and error["link"] is not None, "test_amatreader_no_network returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_amatreader_no_network did not get the expected CyFailedError exception"

    def test_amatreader_exception(self):
        try:
            _bad_amatreader.import_matrix()
        except CyFailedCIError:
            assert False, "test_amatreader_exception got unexpected CyFailedError"
        except BaseException:
            pass
        else:
            assert False, "test_amatreader_exception expected exception"


def suite():
    amatreader_suite = unittest.makeSuite(AMatReaderTestCase, "test")
    return unittest.TestSuite((amatreader_suite))


if __name__ == "__main__":
    unittest.TextTestRunner().run(suite())
