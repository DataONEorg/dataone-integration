#!/usr/bin/env python
# -*- coding: utf-8 -*-

# This work was created by participants in the DataONE project, and is
# jointly copyrighted by participating institutions in DataONE. For
# more information on DataONE, see our web site at http://dataone.org.
#
#   Copyright 2011
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
'''
Module d1_integration.test_all
==============================

Run all integration tests using the unit test framework.

:Created: 2011-04-22
:Author: DataONE (dahl)
:Dependencies:
  - python 2.6
'''

# Std.
import os
import sys
import logging
import unittest
import json

# App.
import test_utilities
from d1_common import xmlrunner
from d1_common import svnrevision
import context

sys.path.append('../client')

# Tier 1

# MN_core.ping()
# MN_core.getLogRecords()
# MN_core.getObjectStatistics()
# MN_core.getOperationStatistics()
# MN_core.getStatus()
# MN_core.getCapabilities()
#
# MN_read.get()
# MN_read.getSystemMetadata()
# MN_read.describe()
# MN_read.getChecksum()
# MN_read.listObjects()
# MN_read.synchronizationFailed()

from tier_1_mn_core_ping import Test010Ping
from tier_1_mn_core_getstatus import Test015GetStatus
from tier_1_mn_core_getcapabilities import Test020GetCapabilities
from tier_1_mn_core_getobjectstatistics import Test025GetObjectStatistics
from tier_1_mn_core_getoperationstatistics import Test026GetOperationStatistics
from tier_1_mn_read_listobjects import Test030ListObjects
from tier_1_mn_core_getlogrecords import Test040GetLogRecords
from tier_1_mn_read_get import Test050Get

#from tier_1_mn_read_describe import TestDescribe
#from tier_1_mn_read_getchecksum import TestGetChecksum
#from tier_1_mn_read_getsystemmetadata import TestGetSystemMetadata
#from tier_1_mn_read_synchronizationfailed import TestSynchronizationFailed

# Tier 2

# Tier 3
#from tier_3_mn_storage_create.py import TestCreate
#from tier_3_mn_storage_update.py import TestUpdate

# Tier 4


def run_tests(node):
  '''Run all tests for one node.
  '''

  context.TOKEN = '<dummy token>'
  context.node = node

  argv = sys.argv
  if "--debug" in argv:
    logging.basicConfig(level=logging.DEBUG)
    argv.remove("--debug")

  suite = unittest.TestLoader().loadTestsFromNames(
    [os.path.splitext(os.path.basename(__file__))[0]])

  if "--with-xunit" in argv:
    argv.remove("--with-xunit")
    xmlrunner.XmlTestRunner(sys.stdout).run(suite)
  else:
    unittest.TextTestRunner(verbosity=2).run(suite)


def main():
  '''Run all tests for each node in the nodes list.
  '''

  nodes_path = test_utilities.get_resource_path('nodes.json')
  with open(nodes_path) as nodes_file:
    nodes = json.load(nodes_file)

  context.nodes = nodes

  for node, val in nodes['mn'].items():
    if node != 'local_gmn_1':
      continue

    run_tests(val)

if __name__ == "__main__":
  main()
