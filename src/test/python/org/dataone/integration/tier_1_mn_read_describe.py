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
:mod:`tier_1_mn_read_describe`
==============================

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


class TestDescribe(d1_test_case.D1TestCase):
  def setUp(self):
    pass

  def describe(self):
    '''
    '''
    client = test_client.TestClient(context.node['baseurl'])
    # Find the PID for a random object that exists on the server.
    pid = self.find_valid_pid(client)
    # Get header information for object.
    info = client.describe(pid)
    self.assertTrue(re.search(r'Content-Length', str(info)))
    self.assertTrue(re.search(r'Date', str(info)))
    self.assertTrue(re.search(r'Content-Type', str(info)))
