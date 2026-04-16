// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

import './App.css';
import appDataProvider from './providers/dataProvider';
import { BrowserRouter } from 'react-router-dom';
import {
    AdminContext,
    AdminUI,
    Loading,
    Resource,
    defaultTheme,
    localStorageStore,
    usePermissions,
} from 'react-admin';
import {
    SchemaList,
    SchemaEdit,
    SchemaCreate,
    SchemaShow,
} from './resources/crs';


import Login from './pages/Login';
import { useUpdateCrdIds } from './hooks/useUpdateCrdIds';
import { i18nProvider } from './providers/i18nProvider';

import SettingsIcon from '@mui/icons-material/Settings';
import LinkIcon from '@mui/icons-material/Link';
import AppIcon from '@mui/icons-material/Apps';
import AlbumIcon from '@mui/icons-material/Album';
import KeyIcon from '@mui/icons-material/Key';
import ModelTraininigIcon from '@mui/icons-material/ModelTraining';
import PieChartIcon from '@mui/icons-material/PieChart';

import MyLayout from './Layout';
import AppDashboard from './pages/Dashboard';
import { CrdShow } from './resources/crd';
import { useContext, useState } from 'react';
import { View, ViewsContext, fetchViews } from './resources';

//import config
import { Config } from './providers/configProvider';
import { buildAuthProvider } from './providers/authProvider';
import crPostgres from './resources/custom/cr.postgres.db.movetokube.com';
import crPostgresUsers from './resources/custom/cr.postgresusers.db.movetokube.com';
import crApiGateways from './resources/custom/cr.apigws.operator.scc-digitalhub.github.io';
import crPostgrest from './resources/custom/cr.postgrests.operator.postgrest.org';
import crDremiorest from './resources/custom/cr.dremiorestservers.operator.dremiorestserver.com';
import crMinioBuckets from './resources/custom/cr.buckets.minio.scc-digitalhub.github.io';
import crMinioUsers from './resources/custom/cr.users.minio.scc-digitalhub.github.io';
import crMinioPolicies from './resources/custom/cr.policies.minio.scc-digitalhub.github.io';
import crHttpRoutes, { GWPolicyView } from './resources/custom/cr.httproutes.gateway.networking.k8s.io';

import { httpClientProvider } from './providers/httpClientProvider';

import { K8SDeploymentList, K8SDeploymentShow } from './resources/k8s/k8s_deployment';
import { K8SPvcCreate, K8SPvcList, K8SPvcShow } from './resources/k8s/k8s_pvc';
import { K8SServiceList, K8SServiceShow } from './resources/k8s/k8s_service';
import { K8SSecretCreate, K8SSecretList, K8SSecretShow } from './resources/k8s/k8s_secret';
import { K8SJobList, K8SJobShow } from './resources/k8s/k8s_job';
import { K8SQuotaList, K8SQuotaShow } from './resources/k8s/k8s_quota';

console.log('Config', Config);

//build providers
const authProvider = buildAuthProvider(Config);

//build http client for provider
const httpClient = httpClientProvider(authProvider);
const dataProvider = appDataProvider(Config.application.apiUrl, httpClient);
const store = localStorageStore();

const customViews: { [index: string]: View } = {
    'postgres.db.movetokube.com': crPostgres,
    'postgresusers.db.movetokube.com': crPostgresUsers,
    'buckets.minio.scc-digitalhub.github.io': crMinioBuckets,
    'users.minio.scc-digitalhub.github.io': crMinioUsers,
    'policies.minio.scc-digitalhub.github.io': crMinioPolicies,
    'apigws.operator.scc-digitalhub.github.io': crApiGateways,
    'postgrests.operator.postgrest.org': crPostgrest,
    'dremiorestservers.operator.dremiorestserver.com': crDremiorest,
    'httproutes.gateway.networking.k8s.io': crHttpRoutes,
    'securitypolicies.gateway.envoyproxy.io': GWPolicyView,
};

//theming
export const themeOptions = {
    ...defaultTheme,
    palette: {
        primary: {
          main: '#AD530F'  
        },
        secondary: {
            main: '#DB6A13',
            highlight: 'rgba(219,106,19, 0.3)'
        },
    },
    sidebar: {
        width: 320, // The default value is 240
    },
};

function App() {
    return (
        <BrowserRouter basename={Config.application.contextPath}>
            <AdminContext
                dataProvider={dataProvider}
                authProvider={authProvider}
                i18nProvider={i18nProvider}
                store={store}
                theme={themeOptions}
            >
                <DynamicAdminUI />
            </AdminContext>
        </BrowserRouter>
    );
}

function DynamicAdminUI() {
    const [views, setViews] = useState<View[]>([]);
    const { crdIds } = useUpdateCrdIds();
    const { permissions } = usePermissions();
    const canAccess = (res: string, op: string) => permissions && permissions.canAccess(res, op)

    const viewsContext = useContext(ViewsContext);

    if (crdIds.length === 0 && views.length > 0) {
        setViews([]);
    } else {
        for (const crdId of crdIds) {
            try {
                if (
                    crdId in customViews &&
                    !viewsContext.list().some(v => v.key === crdId)
                ) {
                    viewsContext.put(customViews[crdId]);
                }
            } catch (error) {
                console.log('No custom view for', crdId);
            }
        }
        if (
            views.length !== crdIds.length ||
            !views.every((s: View) => crdIds.includes(s.key))
        ) {
            setViews(fetchViews(crdIds));
        }
    }

    return (
        <AdminUI
            ready={Loading}
            dashboard={AppDashboard}
            loginPage={Login}
            layout={MyLayout}
            requireAuth={true}
            disableTelemetry={true}
        >
            {views.map((v: any) => (
                <Resource
                    name={v.key}
                    options={{ label: v.name }}
                    key={v.key}
                    list={v.list}
                    show={v.show}
                    create={v.create}
                    edit={v.edit}
                    icon={v.icon}
                />
            ))}
            <Resource
                name="crs"
                list={SchemaList}
                edit={canAccess('crs', 'write') ? SchemaEdit : <></>}
                create={canAccess('crs', 'write') ? SchemaCreate : <></>}
                show={canAccess('crs', 'read') ? SchemaShow : <></>}
                icon={SettingsIcon}
            />
            <Resource name="crd" show={CrdShow} recordRepresentation="id" />
            <Resource
                name="k8s_service"
                list={canAccess('k8s_service', 'list') ? K8SServiceList : <></>}
                show={canAccess('k8s_service', 'read') ? K8SServiceShow : <></>}
                icon={LinkIcon}
            />
            <Resource
                name="k8s_deployment"
                list={canAccess('k8s_deployment', 'list') ? K8SDeploymentList : <></>}
                show={canAccess('k8s_deployment', 'read') ? K8SDeploymentShow : <></>}
                icon={AppIcon}
            />
            <Resource
                name="k8s_job"
                list={canAccess('k8s_job', 'list') ?  K8SJobList : <></>}
                show={canAccess('k8s_job', 'read') ? K8SJobShow : <></>}
                icon={ModelTraininigIcon}
            />
            <Resource
                name="k8s_pvc"
                create={canAccess('k8s_pvc', 'write') ? K8SPvcCreate : <></>}
                list={canAccess('k8s_pvc', 'list') ? K8SPvcList : <></>}
                show={canAccess('k8s_pvc', 'read') ? K8SPvcShow : <></>}
                icon={AlbumIcon}
            />
            <Resource
                name="k8s_secret"
                create={canAccess('k8s_secret', 'write') ? K8SSecretCreate : <></>}
                list={canAccess('k8s_secret', 'list') ? K8SSecretList : <></>}
                show={canAccess('k8s_secret', 'read') ? K8SSecretShow  : <></>}
                icon={KeyIcon}
            />
            <Resource
                name="k8s_quota"
                list={canAccess('k8s_quota', 'list') ? K8SQuotaList : <></>}
                show={canAccess('k8s_quota', 'read') ? K8SQuotaShow : <></>}
                icon={PieChartIcon}
            />
        </AdminUI>
    );
}

export default App;
