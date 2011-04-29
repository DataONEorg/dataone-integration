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
:mod:`tier_3_mn_storage_create`
===============================

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


class TestCreate(d1_test_case.D1TestCase):
  def test_(self):
    pass

  def test_1010_managed_C_create_objects(self):
    '''Managed: Populate MN with set of test objects (local).
    '''
    client = test_client.TestClient(context.node['baseurl'])
    for sysmeta_path in sorted(glob.glob(os.path.join(self.opts.obj_path, '*.sysmeta'))):
      # Get name of corresponding object and open it.
      object_path = re.match(r'(.*)\.sysmeta', sysmeta_path).group(1)
      object_file = open(object_path, 'r')

      # The pid is stored in the sysmeta.
      sysmeta_file = open(sysmeta_path, 'r')
      sysmeta_xml = sysmeta_file.read()
      sysmeta_obj = d1_client.systemmetadata.SystemMetadata(sysmeta_xml)

      # To create a valid URL, we must quote the pid twice. First, so
      # that the URL will match what's on disk and then again so that the
      # quoting survives being passed to the web server.
      #obj_url = urlparse.urljoin(self.opts.obj_url, urllib.quote(urllib.quote(pid, ''), ''))

      # To test the MIME Multipart poster, we provide the Sci object as a file
      # and the SysMeta as a string.
      try:
        response = client.createResponse(context.TOKEN, sysmeta_obj.identifier, object_file, sysmeta_xml, {})
      except Exception as e:
        open('out.htm', 'w').write(e.traceInformation)
        raise
      #  print response.read()
