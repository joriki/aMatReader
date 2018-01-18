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


class AMatReaderTestCase(unittest.TestCase):
    #_SESSION_FILE = "/git/cytoscape/cytoscape/gui-distribution/assembly/target/cytoscape/sampleData/galFiltered.cys" # this must be modified if your local galFiltered.cys is not in this location.

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_amatreader_no_file(self):
        data = {
          "filename": "",
          "delimiter": "TAB",
          "undirected": False,
          "interactionName": "interacts with",
          "headerColumn": "NAMES",
          "headerRow": "NAMES"
        }
        try:
            _amatreader.import_matrix(data)
        except CyFailedCIError as e:
            error = e.args[0]
            assert error["type"] == "urn:cytoscape:ci:amatreader-app:v1:amatreader_current_view:1" \
                   and error["status"] == codes.NOT_FOUND \
                   and error["message"] is not None \
                   and error["link"] is not None, "test_amatreader_no_network returned invalid CyFaileError: " + str(e)
        else:
            assert False, "test_amatreader_no_network did not get the expected CyFailedError exception"

    def test_amatreader(self):
        _amatreader.import_matrix(data)

    def test_amatreader_extend(self):
        AMATREADER_COLUMN = "sample"
        data = {
  "filename": "/Users/bsettle/Desktop/adjs/sample.mat",
  "delimiter": "TAB",
  "undirected": False,
  "interactionName": "interacts with",
  "headerColumn": "NAMES",
  "headerRow": "NAMES"
}
        
        result = _amatreader.extend_matrix(data)
        pass

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
