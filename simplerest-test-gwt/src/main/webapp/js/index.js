import Vue from 'vue'
import Vuetify from 'vuetify'
import 'vuetify/dist/vuetify.min.css'
import '@mdi/font/css/materialdesignicons.css'

import VueRouter from 'vue-router';

import './polyfill';
import '../css/todo.css';

import '../../../../node_modules/ag-grid-community/dist/styles/ag-grid.css';
import '../../../../node_modules/ag-grid-community/dist/styles/ag-theme-material.css';
import {AgGridVue} from 'ag-grid-vue';

import 'es7-object-polyfill';

Vue.use(Vuetify);

Vue.use(VueRouter);

Vue.component("AgGridVue", AgGridVue);


window.Vue = Vue;
window.Vuetify = Vuetify;
window.VueRouter = VueRouter;



