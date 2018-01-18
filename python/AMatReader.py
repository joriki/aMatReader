import json

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
        return self._cy_caller.execute_post("/aMatReader/v1/extend/" + suid,
                                            json.dumps(data))
