#!/usr/bin/env python
# -*- coding: utf-8 -*-

# This work was created by participants in the DataONE project, and is
# jointly copyrighted by participating institutions in DataONE. For
# more information on DataONE, see our web site at http://dataone.org.
#
#   Copyright ${year}
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
:mod:`tier_1_mn_read_getchecksum`
=================================

:Created: 2011-04-22
:Author: DataONE (dahl)
:Dependencies:
  - python 2.6
'''

# Std.
import sys
import logging
import unittest

# D1.
from d1_common import xmlrunner
import d1_common.const
import d1_common.types.exceptions
import d1_test_case

# App.
import context
import test_client
import test_utilities


class Test070GetChecksum(d1_test_case.D1TestCase):
  def setUp(self):
    pass


  def test_010_get_checksum_by_invalid_pid(self):
    '''404 NotFound when attempting to get checksum for non-existing object.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    self.assertRaises(d1_common.types.exceptions.NotFound, client.get,
                      context.TOKEN, '_invalid_pid_')


  def test_020_get_object_by_valid_pid(self):
    '''Successful retrieval of checksums for known objects.
    '''
    # Verify that the checksums retrieved by getChecksum match what listObjects
    # reported.
    for object_list in context.slices:
      for object_info in object_list.objectInfo:
        client = test_client.TestClient(context.node['baseurl'])
        pid = object_info.identifier.value()
        checksum = client.getChecksum(context.TOKEN, pid)
        self.assertEqual(object_info.checksum.value(), checksum.value())
        self.assertEqual(object_info.checksum.algorithm, checksum.algorithm)
