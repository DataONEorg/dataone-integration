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
:mod:`tier_1_mn_core_getoperationstatistics`
============================================

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


class Test026GetOperationStatistics(d1_test_case.D1TestCase):
  def setUp(self):
    pass


  def test_010_cumulative_no_filter(self):
    '''Monitor Events: Cumulative, no filter.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getOperationStatistics(context.TOKEN)
    self.assertEqual(len(monitor_list.monitorInfo), 1)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, EVENTS_TOTAL_EVENT_TOTAL_NO_FILTER)


  def test_020_cumulative_filter_by_time(self):
    '''Monitor Events: Cumulative, filter by event time.
    '''
    # TODO: Story #1424: Change to use the standard ISO 8601 time interval notation.
    pass


  def test_030_cumulative_filter_by_event_type(self):
    '''Monitor Events: Cumulative, filter by event type.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getOperationStatistics(context.TOKEN,
                                              event='read')
    self.assertEqual(len(monitor_list.monitorInfo), 1)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, EVENTS_READ)


  def test_040_cumulative_filter_by_object_format(self):
    '''Monitor Events: Cumulative, filter by time and format.
    '''
    # TODO: Test set currently contains only one format. Create
    # some more formats so this can be tested properly.
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getOperationStatistics(context.TOKEN,
                                                 format='eml://ecoinformatics.org/eml-2.0.0')
    self.assertEqual(len(monitor_list.monitorInfo), 1)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, EVENTS_TOTAL_EVENT_TOTAL_TIME_FORMAT)


  def test_050_cumulative_filter_by_principal(self):
    '''Monitor Events: Cumulative, filter by event PID.
    '''
    # TODO: Ticket
    pass


  def test_060_daily_no_filter(self):
    '''Monitor Events: Daily, no filter.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getOperationStatistics(context.TOKEN, day=True)
    #self.assertEqual(len(monitor_list.monitorInfo), EVENTS_UNIQUE_DATES)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    found_date = False
    for monitor_info in monitor_list.monitorInfo:
      if str(monitor_info.date) == '1981-08-28':
        found_date = True
        self.assertEqual(monitor_info.count, 1)
    self.assertTrue(found_date)


  def test_070_daily_filter_by_time_1(self):
    '''Monitor Events: Daily, filter by event creation time. 1980->.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getOperationStatistics(context.TOKEN,
                                                 fromDate=datetime.datetime(1980, 1, 1))
    self.assertEqual(len(monitor_list.monitorInfo), 1)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, EVENTS_TOTAL_EVENT_UNI_TIME_FROM_1980)


  def test_080_daily_filter_by_time_2(self):
    '''Monitor Events: Daily, filter by event creation time. 1980-1990.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getOperationStatistics(context.TOKEN,
                                                 fromDate=datetime.datetime(1980, 1, 1),
                                                 toDate=datetime.datetime(1990, 1, 1))
    self.assertEqual(len(monitor_list.monitorInfo), 1)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, EVENTS_TOTAL_EVENT_UNI_TIME_FROM_1980_TO_1990)


  def test_090_daily_filter_by_time_3(self):
    '''Monitor Events: Daily, filter by event creation time. <-1990.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getOperationStatistics(context.TOKEN,
                                                 toDate=datetime.datetime(1990, 1, 1))
    self.assertEqual(len(monitor_list.monitorInfo), 1)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, EVENTS_TOTAL_EVENT_UNI_TIME_TO_1990)


  def test_100_daily_filter_by_event_type(self):
    '''Monitor Events: Daily, filter by event format.
    '''
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getOperationStatistics(context.TOKEN,
                                              event='read',
                                              day=True)
    #self.assertEqual(len(monitor_list.monitorInfo), EVENTS_UNIQUE_DATES_WITH_READ)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    self.assertEqual(monitor_list.monitorInfo[0].count, 1)


  def test_110_daily_filter_by_object_format(self):
    '''Monitor Events: Daily, filter by event format.
    '''
    # TODO: Test set currently contains only one format. Create
    # some more formats so this can be tested properly.
    client = test_client.TestClient(context.node['baseurl'])
    monitor_list = client.getOperationStatistics(context.TOKEN,
                                              format='eml://ecoinformatics.org/eml-2.0.0',
                                              day=True)
    #self.assertEqual(len(monitor_list.monitorInfo), EVENTS_WITH_OBJECT_FORMAT_EML)
    self.assert_valid_date(str(monitor_list.monitorInfo[0].date))
    #self.assertEqual(monitor_list.monitorInfo[0].count, EVENTS_COUNT_OF_FIRST)


  def test_120_daily_filter_by_principal(self):
    '''Monitor Events: Daily, filter by time and format.
    '''
    # TODO: Story
    pass
