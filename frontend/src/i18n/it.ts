// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

import italianMessages from '@dslab/ra-language-italian';

const messages = {
    ...italianMessages,
    login: {
        basicMessage: 'Autenticarsi per continuare',
        title: 'Resource Manager',
        message: 'Accedi con AAC',
    },
    resources: {
        crs: {
            name: 'Schema |||| Impostazioni',
            schemas: 'Schemi',
            fields: {
                id: 'ID',
                crdId: 'CRD',
                version: 'Versione',
                schema: 'Schema',
            },
            listSubtitle:
                'Aggiungere uno schema per abilitare le CR corrispondenti.',
            createVersionHelp: 'Selezionare una CRD',
        },
        crd: {
            name: 'CRD',
            fields: {
                metadata: {
                    creationTimestamp: 'Creazione',
                    generation: 'Generazione',
                    name: 'Nome',
                    resourceVersion: 'Versione risorsa',
                    uid: 'UID',
                },
                managedFields: 'Campi gestiti',
            },
        },
        cr: {
            fields: {
                id: 'ID',
                apiVersion: 'Versione API',
                kind: 'Tipo',
                metadata: 'Metadata',
                'metadata.name': 'Nome',
                spec: 'Specifiche',
            },
            serverError: 'Errore dal server'
        },
        k8s_service: {
            name: 'Servizio |||| Servizi',
            fields: {
                metadata: {
                    creationTimestamp: 'Creazione',
                    generation: 'Generazione',
                    name: 'Nome',
                    resourceVersion: 'Versione risorsa',
                    uid: 'UID',
                },
                spec: {
                    'ports[0].port': 'Porta',
                    'ports[0].name': 'Nome porta' 
                },
                type: 'Tipo',
                types: 'Tipi'
            },
        },
        k8s_job: {
            name: 'Job |||| Job',
            fields: {
                metadata: {
                    creationTimestamp: 'Creazione',
                    generation: 'Generazione',
                    name: 'Nome',
                    resourceVersion: 'Versione risorsa',
                    uid: 'UID',
                },
                completion: 'Completamento',
                duration: 'Durata',
                types: 'Tipi'
            },
        },
        k8s_deployment: {
            name: 'Deployment |||| Deployment',
            fields: {
                metadata: {
                    creationTimestamp: 'Creazione',
                    generation: 'Generazione',
                    name: 'Nome',
                    resourceVersion: 'Versione risorsa',
                    uid: 'UID',
                },
                status: 'Ready',
                types: 'Tipi'
            },
        },
        k8s_quota: {
            name: 'Resource Quota |||| Resource Quota',
            fields: {
                metadata: {
                    creationTimestamp: 'Creazione',
                    generation: 'Generazione',
                    name: 'Nome',
                    resourceVersion: 'Versione risorsa',
                    uid: 'UID',
                },
                resource: 'Risorsa',
                used: 'Uso',
                limit: 'Limite',
            },
        },
        k8s_pvc: {
            name: 'Persistent Volume Claim |||| Persistent Volume Claim',
            fields: {
                metadata: {
                    creationTimestamp: 'Creazione',
                    generation: 'Generazione',
                    name: 'Nome',
                    resourceVersion: 'Versione risorsa',
                    uid: 'UID',
                },
                status: {
                    phase: 'Stato',
                },
                spec: {
                    storageClassName: 'Classe di Storage',
                    volumeName: 'Volume',
                    volumeMode:'Modo',
                    accessModes: 'Modi di accesso',
                    resources: {
                        requests: {
                            storage: 'Spazio (Gi)'
                        }
                    }
                }
            },
        },
        k8s_secret: {
            name: 'Segreto |||| Segreti',
            fields: {
                metadata: {
                    creationTimestamp: 'Creazione',
                    generation: 'Generazione',
                    name: 'Nome',
                    resourceVersion: 'Versione risorsa',
                    uid: 'UID',
                },
                type: 'Tipo',
                data: 'Dati',
                secretname: 'Nome segreto',
                secretnum: 'Numero delle chiavi'
            },
            decode: 'Decodifica'
        },
        'postgres.db.movetokube.com': {
            name: 'DB Postgres',
            fields: {
                id: 'ID',
                spec: {
                    database: 'Database',
                    dropOnDelete: 'Elimina alla cancellazione',
                    extensions: 'Estensioni',
                    masterRole: 'Ruolo master',
                    schemas: 'Schemi',
                    masterRoleHint: 'Role to set as DB owner. Defaults to <database-name>-group',
                    databaseHint: 'In base a questo nome, si creano i ruoli owner, reader, e writer.',
                    extensionsHint: 'Lista delle estensioni (separati da virgola) da installare opzionalmente.',
                    schemasHint: 'Lista di schemi (separati da virgola) da creare opzionalmente.',

                },
            },
        },
        'postgresusers.db.movetokube.com': {
            name: 'Utente Postgres |||| Utenti Postgres',
            shortName: 'Utenti',
            fields: {
                id: 'ID',
                spec: {
                    database: 'ID Database',
                    privileges: 'Privilegi',
                    role: 'Ruolo',
                    secretName: 'Nome secret',
                    roleHint: 'Nome utente per DB login',
                    secretNameHint: 'Il secret si crea con la forma <metadata-name>-<secret-name>',

                },
            },
        },
        'buckets.minio.scc-digitalhub.github.io': {
            name: 'S3 Bucket |||| S3 Bucket',
            fields: {
                id: 'ID',
                spec: {
                    name: 'Bucket',
                    quota: 'Quota (Byte)',
                    nameHint: 'Nome Bucket',
                    quotaHint: 'Quota in Byte',
                },
            },
            errors: {
                'quota': 'Quota deve essere un numero positivo',
            }

        },
        'users.minio.scc-digitalhub.github.io': {
            name: 'Utente S3 |||| Utenti S3',
            fields: {
                id: 'ID',
                spec: {
                    accessKey: 'Access key',
                    secretKey: 'Secret key',
                    policies: 'Policy',
                    policyHint: 'Elenco delle policy',
                },
            },
        },
        'policies.minio.scc-digitalhub.github.io': {
            name: 'S3 Policy |||| S3 Policy',
            fields: {
                id: 'ID',
                spec: {
                    name: 'Policy',
                    content: 'Contenuto di policy (JSON)',
                    nameHint: 'Nome di policy',
                },
            },
        },
        'postgrests.operator.postgrest.org': {
            name: 'Servizio Dati PostgREST |||| Servizi Dati PostgREST',
            shortName: 'PostgREST',
            fields: {
                id: 'ID',
                spec: {
                    database: 'Database ID',
                    schema: 'Schema DB',
                    anonRole: 'Nome utente DB esistente',
                    grants: 'Permessi DB',
                    tables: 'Tabelle DB esposte',
                    connection: {
                        title: 'Connessione',
                        host: 'DB Host',
                        hostHint: 'Obbligatorio se secret non ha POSTGREST_URL',
                        port: 'DB Port',
                        database: 'Nome database',
                        databaseHint: 'Obbligatorio se secret non ha POSTGREST_URL',
                        secretName: 'Nome segreto',
                        secretNameHint: 'Secret deve definire POSTGRES_URL oppure i valori di USER e PASSWORD del database',
                        user: 'Utente DB',
                        password: 'Password DB',
                        extraParams: 'Ulteriori parametri di connessione'
                    }
                },
                status: {
                    state: 'Stato',
                    message: 'Messaggio relativo allo stato'
                },
                tables: 'Tabelle DB esposte',
                grants: 'Permessi DB',
                existing: 'Con utente DB esistente',
                existingSecret: 'Con segreto esistente'
            },
        },
        'dremiorestservers.operator.dremiorestserver.com': {
            name: 'Servizio Dati Dremio |||| Servizi Dati Dremio',
            shortName: 'Servizio Dati Dremio',
            fields: {
                id: 'ID',
                spec: {
                    tables: 'Virtual set Dremio esposti',
                    connection: {
                        title: 'Connessione',
                        host: 'Dremio Host',
                        port: 'Dremio Port',
                        secretName: 'Nome segreto',
                        user: 'Utente Dremio',
                        password: 'Password Dremio',
                        jdbcProperties: 'Ulteriori parametri di connessione'
                    }
                },
                status: {
                    state: 'Stato'
                },
                tables: 'Virtual set Dremio esposti',
                existingSecret: 'Con segreto esistente'
            },
        },
        'nuclioapigateways.nuclio.io': {
            name: 'API Gateway',
            fields: {
                id: 'ID',
                spec: {
                    authentication: {
                        basicAuth: {
                            username: 'Nome utente',
                            password: 'Password',
                        },
                        dexAuth: {
                            oauth2ProxyUrl: 'OAuth2 proxy URL',
                            redirectUnauthorizedToSignIn: 'Reindirizzare al sign in quando non autorizzato'
                        },
                        jwtAuth: {
                            audience: 'Audience'
                        }
                    },
                    authenticationMode: 'Modalità',
                    host: 'Host',
                    name: 'Nome',
                    description: 'Descrizione',
                    path: 'Percorso',
                    upstreams: 'Upstream',
                    'upstreams.kind': 'Tipo',
                    'upstreams.nucliofunction.name': 'Nome funzione Nuclio',
                    'upstreams.service.name': 'Nome servizio',
                },
                status: {
                    state: 'Stato'
                }
            },
            authenticationTitle: 'Autenticazione',
            alreadyExists: 'Esiste già'
        },
        'apigws.operator.scc-digitalhub.github.io': {
            name: 'API Gateway |||| API Gateway',
            fields: {
                id: 'ID',
                spec: {
                    auth: {
                        type: 'Modalità',
                        basic: {
                            user: 'Utente',
                            password: 'Password',
                        },
                    },
                    host: 'Host',
                    path: 'Percorso',
                    port: 'Porta',
                    service: 'Servizio',
                },
                status: {
                    state: 'Stato'
                }
            },
            authenticationTitle: 'Autenticazione'
        },
        'httproutes.gateway.networking.k8s.io': {
            name: 'HTTP Route |||| HTTP Route',
            fields: {
                id: 'ID',
                spec: {
                    hostname: 'Hostname',
                    backendService: 'Servizio Backend',
                    backendPort: 'Porta Backend',
                    path: 'Prefisso percorso match',
                    backendPath: 'Prefisso percorso riscrittura',
                    gatewayName: 'Gateway',
                    auth: {
                        type: 'Autenticazione',
                        secretName: 'Secret credenziali',
                        headerName: 'Header API Key',
                        issuer: 'Issuer JWT',
                        jwksUri: 'URI JWKS',
                        audiences: 'Audience',
                        claimToHeader: 'Claim "%{claim}" → Header',
                    },
                },
            },
            authenticationTitle: 'Autenticazione',
        },
    },
    dashboard: {
        title: 'Resource Manager',
        message: 'Benvenuto in Resource Manager.',
        emptyTitle: 'Non ci sono ancora Custom Resource.',
        emptySubtitle: 'Devi creare uno schema prima di creare delle Custom Resource.',
        goToResource: 'Vai alla risorsa'
    },
    buttons: {
        copy: 'Copia',
        listCrs: 'Elenco custom resources',
        createUser: 'Aggiungi utente',
    },
    label: {
        name: 'Nome Label',
        value: 'Valore Label'
    },
    clipboard: {
        copied: 'Valore copiato'
    }

};

export default messages;
