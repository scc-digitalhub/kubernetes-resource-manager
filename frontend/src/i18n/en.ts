// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

import { access } from 'fs';
import englishMessages from 'ra-language-english';

const messages = {
    ...englishMessages,
    login: {
        basicMessage: 'Please log in to continue',
        title: 'Resource Manager',
        message: 'Log in with AAC',
    },
    resources: {
        crs: {
            name: 'Schema |||| Settings',
            schemas: 'Schemas',
            fields: {
                id: 'ID',
                crdId: 'CRD',
                version: 'Version',
                schema: 'Schema',
            },
            listSubtitle: 'Please add a schema to enable CR management.',
            createVersionHelp: 'Please select a CRD',
        },
        crd: {
            name: 'CRD |||| CRDs',
            fields: {
                metadata: {
                    creationTimestamp: 'Created',
                    generation: 'Generation',
                    name: 'Name',
                    resourceVersion: 'Resource version',
                    uid: 'UID',
                },
                managedFields: 'Managed fields',
            },
        },
        cr: {
            fields: {
                id: 'ID',
                apiVersion: 'API Version',
                kind: 'Kind',
                metadata: 'Metadata',
                'metadata.name': 'Name',
                spec: 'Specification',
            },
            serverError: 'Server error'
        },
        k8s_service: {
            name: 'Service |||| Services',
            fields: {
                metadata: {
                    creationTimestamp: 'Created',
                    generation: 'Generation',
                    name: 'Name',
                    resourceVersion: 'Resource version',
                    uid: 'UID',
                },
                spec: {
                    'ports[0].port': 'Port',
                    'ports[0].name': 'Port name' 
                },
                type: 'Type',
                types: 'Types'
            },
        },
        k8s_job: {
            name: 'Job |||| Jobs',
            fields: {
                metadata: {
                    creationTimestamp: 'Created',
                    generation: 'Generation',
                    name: 'Name',
                    resourceVersion: 'Resource version',
                    uid: 'UID',
                },
                completion: 'Completion',
                duration: 'Duration',
                types: 'Types'
            },
        },
        k8s_deployment: {
            name: 'Deployment |||| Deployments',
            fields: {
                metadata: {
                    creationTimestamp: 'Created',
                    generation: 'Generation',
                    name: 'Name',
                    resourceVersion: 'Resource version',
                    uid: 'UID',
                },
                status: 'Ready',
                types: 'Types'
            },
        },
        k8s_quota: {
            name: 'Resource Quota |||| Resource Quotas',
            fields: {
                metadata: {
                    creationTimestamp: 'Created',
                    generation: 'Generation',
                    name: 'Name',
                    resourceVersion: 'Resource version',
                    uid: 'UID',
                },
                resource: 'Resource',
                used: 'Used',
                limit: 'Limit',
            },
        },
        k8s_pvc: {
            name: 'Persistent Volume Claim |||| Persistent Volume Claims',
            fields: {
                metadata: {
                    creationTimestamp: 'Created',
                    generation: 'Generation',
                    name: 'Name',
                    resourceVersion: 'Resource version',
                    uid: 'UID',
                },
                status: {
                    phase: 'Status',
                },
                spec: {
                    storageClassName: 'Storage Class',
                    volumeName: 'Volume',
                    volumeMode:'Mode',
                    accessModes: 'Access Modes',
                    resources: {
                        requests: {
                            storage: 'Space (Gi)'
                        }
                    }
                }
            },
        },
        k8s_secret: {
            name: 'Secret |||| Secrets',
            fields: {
                metadata: {
                    creationTimestamp: 'Created',
                    generation: 'Generation',
                    name: 'Name',
                    resourceVersion: 'Resource version',
                    uid: 'UID',
                },
                type: 'Type',
                data: 'Data',
                secretname: 'Secret Name',
                secretnum: 'Number of Secrets'
            },
            decode: 'Decode'
        },
        'postgres.db.movetokube.com': {
            name: 'Postgres DB |||| Postgres DBs',
            fields: {
                id: 'ID',
                spec: {
                    database: 'Database',
                    dropOnDelete: 'Drop on delete',
                    extensions: 'Extensions',
                    masterRole: 'Master role',
                    schemas: 'Schemas',
                    masterRoleHint: 'Role to set as DB owner. Defaults to <database-name>-group',
                    databaseHint: 'Based on this name, the owner, reader, and writer rolers are created.',
                    extensionsHint: 'List of PostgreSQL extensions to optionally install.',
                    schemasHint: 'List of comma-separated schema names to optionally create.',
                },
            },
        },
        'postgresusers.db.movetokube.com': {
            name: 'Postgres user |||| Postgres users',
            shortName: 'Users',
            fields: {
                id: 'ID',
                spec: {
                    database: 'Database ID',
                    privileges: 'Privileges',
                    role: 'Role',
                    secretName: 'Secret name',
                    roleHint: 'Username for DB login',
                    secretNameHint: 'Resulting secret with have a form <metadata-name>-<secret-name>',
                },
            },
        },
        'buckets.minio.scc-digitalhub.github.io': {
            name: 'S3 Bucket |||| S3 Buckets',
            fields: {
                id: 'ID',
                spec: {
                    name: 'Bucket',
                    quota: 'Quota (Bytes)',
                    nameHint: 'Bucket name',
                    quotaHint: 'Quota in Bytes',
                },
            },
            errors: {
                'quota': 'Quota must be a positive number',
            }
        },
        'users.minio.scc-digitalhub.github.io': {
            name: 'S3 User |||| S3 Users',
            fields: {
                id: 'ID',
                spec: {
                    accessKey: 'Access key',
                    secretKey: 'Secret key',
                    policies: 'Policies',
                    policyHint: 'Policies list',
                },
            },
        },
        'policies.minio.scc-digitalhub.github.io': {
            name: 'S3 Policy |||| S3 Policies',
            fields: {
                id: 'ID',
                spec: {
                    name: 'Policy',
                    content: 'Policy content (JSON)',
                    nameHint: 'Policy name',
                },
            },
        },
        'postgrests.operator.postgrest.org': {
            name: 'PostgREST Data Service |||| PostgREST Data Services',
            shortName: 'PostgREST Data Service',
            fields: {
                id: 'ID',
                spec: {
                    database: 'Database ID',
                    schema: 'DB Schema',
                    anonRole: 'Existing DB user name',
                    grants: 'DB permissions',
                    tables: 'Exposed DB tables',
                    connection: {
                        title: 'Connection',
                        host: 'DB Host',
                        hostHint: 'Required if secret has no POSTGREST_URL',
                        port: 'DB Port',
                        database: 'Database name',
                        databaseHint: 'Required if secret has no POSTGREST_URL',
                        secretName: 'Secret name',
                        secretNameHint: 'Secret should contain either full POSTGRES_URL or USER and PASSWORD keys',
                        user: 'DB User',
                        password: 'DB Password',
                        extraParams: 'Extra connection URL params'
                    }
                },
                status: {
                    state: 'Status',
                    message: 'Status message'
                },
                tables: 'Exposed DB tables',
                grants: 'DB permissions',
                existing: 'With existing DB user',
                existingSecret: 'With existing secret'
            },
        },
        'dremiorestservers.operator.dremiorestserver.com': {
            name: 'Dremio Data Service |||| Dremio Data Services',
            shortName: 'Dremio Data Service',
            fields: {
                id: 'ID',
                spec: {
                    tables: 'Exposed Dremio virtual sets',
                    connection: {
                        title: 'Connection',
                        host: 'Dremio Host',
                        port: 'Dremio Port',
                        secretName: 'Secret name',
                        user: 'Dremio User',
                        password: 'Dremio Password',
                        jdbcProperties: 'Extra URL connection parameters'
                    }
                },
                status: {
                    state: 'Status'
                },
                tables: 'Exposed Dremio virtual sets',
                existingSecret: 'With existing secret'
            },
        },
        'nuclioapigateways.nuclio.io': {
            name: 'API Gateway |||| API Gateways',
            fields: {
                id: 'ID',
                spec: {
                    authentication: {
                        basicAuth: {
                            username: 'Username',
                            password: 'Password',
                        },
                        dexAuth: {
                            oauth2ProxyUrl: 'OAuth2 proxy URL',
                            redirectUnauthorizedToSignIn: 'Redirect unauthorized to sign in'
                        },
                        jwtAuth: {
                            audience: 'Audience'
                        }
                    },
                    authenticationMode: 'Mode',
                    host: 'Host',
                    name: 'Name',
                    description: 'Description',
                    path: 'Path',
                    upstreams: 'Upstreams',
                    'upstreams.kind': 'Kind',
                    'upstreams.nucliofunction.name': 'Nuclio function name',
                    'upstreams.service.name': 'Service name',
                },
                status: {
                    state: 'Status'
                }
            },
            authenticationTitle: 'Authentication',
            alreadyExists: 'Already exists'
        },
        'apigws.operator.scc-digitalhub.github.io': {
            name: 'API Gateway |||| API Gateways',
            fields: {
                id: 'ID',
                spec: {
                    auth: {
                        type: 'Mode',
                        basic: {
                            user: 'Username',
                            password: 'Password',
                        },
                    },
                    host: 'Host',
                    path: 'Path',
                    port: 'Port',
                    service: 'Service',
                },
                status: {
                    state: 'Status'
                }
            },
            authenticationTitle: 'Authentication'
        },
        'httproutes.gateway.networking.k8s.io': {
            name: 'HTTP Route |||| HTTP Routes',
            fields: {
                id: 'ID',
                spec: {
                    hostname: 'Hostname',
                    backendService: 'Backend Service',
                    backendPort: 'Backend Port',
                    path: 'Match Path Prefix',
                    backendPath: 'Rewrite Path Prefix',
                    gatewayName: 'Gateway',
                    auth: {
                        type: 'Authentication',
                        secretName: 'Credentials Secret',
                        headerName: 'API Key Header',
                        issuer: 'JWT Issuer',
                        jwksUri: 'JWKS URI',
                        audiences: 'Audiences',
                        claimToHeader: 'Claim "%{claim}" → Header',
                    },
                },
            },
            authenticationTitle: 'Authentication',
        },
    },
    dashboard: {
        title: 'Resource Manager',
        message: 'Welcome to the Resource Manager.',
        emptyTitle: 'There are no Custom Resources yet.',
        emptySubtitle: 'You must create a schema before creating any Custom Resources.',
        goToResource: 'Go to resource'
    },
    buttons: {
        copy: 'Copy',
        listCrs: 'List custom resources',
        createUser: 'Add user',
    },
    label: {
        name: 'Label Name',
        value: 'Label Value'
    },
    clipboard: {
        copied: 'Value copied to clipboard'
    }
};

export default messages;
