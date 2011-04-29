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
:mod:`tier_1_mn_read_getsystemmetadata`
=======================================

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


class TestGetSystemMetadata(d1_test_case.D1TestCase):

  def setUp(self):
    pass


  def test_get_sysmeta_by_invalid_pid(self):
    '''404 NotFound when attempting to get non-existing SysMeta /meta/_invalid_pid_.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    self.assertRaises(d1_common.types.exceptions.NotFound,
                      client.getSystemMetadata,
                      context.TOKEN,
                      '_invalid_pid_')


  def test_get_sysmeta_by_valid_pid(self):
    '''Successful retrieval of valid object /meta/valid_pid.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    response = client.getSystemMetadata(context.TOKEN, '10Dappend2.txt')
    self.assertTrue(response)


  def test_object_properties(self):
    '''Read complete object collection and compare with values stored in local SysMeta files.
    '''
    # Get object collection.
    client = test_client.TestClient(context.node['baseurl'], timeout=60)
    object_list = client.listObjects(context.TOKEN,
                                     count=d1_common.const.MAX_LISTOBJECTS)

    # Loop through our local test objects.
    for sysmeta_path in sorted(glob.glob(os.path.join(self.opts.obj_path, '*.sysmeta'))):
      # Get name of corresponding object and check that it exists on disk.
      object_path = re.match(r'(.*)\.sysmeta', sysmeta_path).group(1)
      self.assertTrue(os.path.exists(object_path))
      # Get pid for object.
      pid = urllib.unquote(os.path.basename(object_path))
      # Get sysmeta xml for corresponding object from disk.
      sysmeta_file = open(sysmeta_path, 'r')
      sysmeta_obj = d1_client.systemmetadata.SystemMetadata(sysmeta_file)

      # Get corresponding object from objectList.
      found = False
      for object_info in object_list.objectInfo:
        if object_info.identifier.value() == sysmeta_obj.identifier:
          found = True
          break;

      self.assertTrue(found, 'Couldn\'t find object with pid "{0}"'.format(sysmeta_obj.identifier))

      self.assertEqual(object_info.identifier.value(), sysmeta_obj.identifier)
      self.assertEqual(object_info.objectFormat, sysmeta_obj.objectFormat)
      self.assertEqual(object_info.dateSysMetadataModified, sysmeta_obj.dateSysMetadataModified)
      self.assertEqual(object_info.size, sysmeta_obj.size)
      self.assertEqual(object_info.checksum.value(), sysmeta_obj.checksum)
      self.assertEqual(object_info.checksum.algorithm, sysmeta_obj.checksumAlgorithm)
