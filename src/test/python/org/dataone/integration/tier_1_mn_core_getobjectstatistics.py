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
:mod:`tier_1_mn_core_getobjectstatistics`
=========================================

:Created: 2011-04-22
:Author: DataONE (dahl)
:Dependencies:
  - python 2.6
'''

# Std.
import datetime
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


class Test025GetObjectStatistics(d1_test_case.D1TestCase):
  def setUp(self):
    pass


  def test_010_cumulative_no_filter(self):
    '''Monitor Objects: Cumulative, no filter.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getObjectStatistics(context.TOKEN)
    self.assertEqual(len(monitor_list.monitorInfo), 1)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, OBJECTS_TOTAL_DATA)


  def test_020_cumulative_filter_by_time(self):
    '''Monitor Objects: Cumulative, filter by object creation time.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getObjectStatistics(context.TOKEN,
                                              fromDate=datetime.datetime(2000, 01, 01),
                                              toDate=datetime.datetime(2005, 01, 01))
    self.assertEqual(len(monitor_list.monitorInfo), 1)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, OBJECTS_TOTAL_DATA)


  def test_030_cumulative_filter_by_format(self):
    '''Monitor Objects: Cumulative, filter by object format.
    '''
    # TODO: Test set currently contains only one format. Create
    # some more formats so this can be tested properly.
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getObjectStatistics(context.TOKEN,
                                              format='eml://ecoinformatics.org/eml-2.0.0')
    self.assertEqual(len(monitor_list.monitorInfo), 1)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, OBJECTS_TOTAL_DATA)


  def test_040_cumulative_filter_by_time_and_format(self):
    '''Monitor Objects: Cumulative, filter by time and format.
    '''
    # TODO: Story #1424
    pass


  def test_050_cumulative_filter_by_pid(self):
    '''Monitor Objects: Cumulative, filter by object PID.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getObjectStatistics(context.TOKEN, pid='f*')
    self.assertEqual(len(monitor_list.monitorInfo), 1)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, OBJECTS_PID_STARTSWITH_F)


  def test_060_daily_no_filter(self):
    '''Monitor Objects: Daily, no filter.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getObjectStatistics(context.TOKEN, day=True)
    #self.assertEqual(len(monitor_list.monitorInfo), OBJECTS_UNIQUE_DATES)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    found_date = False
    for monitor_info in monitor_list.monitorInfo:
      if str(monitor_info.date) == '1982-08-17':
        found_date = True
        self.assertEqual(monitor_info.count, 2)
    self.assertTrue(found_date)


  def test_070_daily_filter_by_time(self):
    '''Monitor Objects: Daily, filter by object creation time.
    '''
    # TODO: Story #1424: Change to use the standard ISO 8601 time interval notation.
    pass


  def test_080_daily_filter_by_format(self):
    '''Monitor Objects: Daily, filter by object format.
    '''
    # TODO: Test set currently contains only one format. Create
    # some more formats so this can be tested properly.
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getObjectStatistics(context.TOKEN,
                                              format='eml://ecoinformatics.org/eml-2.0.0',
                                              day=True)
    #self.assertEqual(len(monitor_list.monitorInfo), OBJECTS_UNIQUE_DATE_AND_FORMAT_EML)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    self.assertEqual(monitor_list.monitorInfo[0].count, 1)


  def test_090_daily_filter_by_time_and_format(self):
    '''Monitor Objects: Daily, filter by time and format.
    '''
    # TODO: Story #1424

    pass

  def test_100_daily_filter_by_pid(self):
    '''Monitor Objects: Daily, filter by object PID.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getObjectStatistics(context.TOKEN, pid='f*', day=True)
    #self.assertEqual(len(monitor_list.monitorInfo), OBJECTS_UNIQUE_DATE_AND_PID_STARTSWITH_F)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    self.assertEqual(monitor_list.monitorInfo[0].count, 1)
