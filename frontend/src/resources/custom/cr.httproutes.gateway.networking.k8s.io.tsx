// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

import {
    Create,
    Datagrid,
    Edit,
    EditButton,
    List,
    ShowButton,
    SimpleForm,
    TextField,
    TextInput,
    Show,
    SimpleShowLayout,
    required,
    DeleteWithConfirmButton,
    useShowController,
    useEditController,
    SelectInput,
    FormDataConsumer,
    useTranslate,
    NumberInput,
    useGetOne,
    usePermissions,
    useCreate,
    useUpdate,
    useDelete,
    useRedirect,
    FunctionField,
    Labeled,
    ReferenceInput,
    AutocompleteInput,
    useRecordContext,
    ArrayField,
    SingleFieldList,
    ChipField,
    ArrayInput,
    SimpleFormIterator,
} from 'react-admin';
import { useFormContext, useWatch } from 'react-hook-form';
import {
    CreateTopToolbar,
    EditTopToolbar,
    ListTopToolbar,
    ShowTopToolbar,
} from '../../components/toolbars';
import { SimplePageTitle } from '../cr';
import { View } from '..';
import { useCrTransform } from '../../hooks/useCrTransform';
import { Box, Grid, Typography } from '@mui/material';
import { useRef } from 'react';
import AltRouteIcon from '@mui/icons-material/AltRoute';
import { Breadcrumb } from '@dslab/ra-breadcrumb';
import { labels2types } from '../../utils';

const CR_HTTPROUTES = 'httproutes.gateway.networking.k8s.io';
const CR_SECURITYPOLICIES = 'securitypolicies.gateway.envoyproxy.io';

const SP_API_VERSION = 'gateway.envoyproxy.io/v1alpha1';
const SP_KIND = 'SecurityPolicy';

/********************** Validation **********************/

const validateFields = (values: any) => {
    const res: any = {};
    if (!values.metadata?.name) res['metadata.name'] = 'ra.validation.required';
    if (!values.spec?.hostname) res['spec.hostname'] = 'ra.validation.required';
    if (!values.spec?.backendService) res['spec.backendService'] = 'ra.validation.required';
    if (!values.spec?.backendPort) res['spec.backendPort'] = 'ra.validation.required';
    if (!values.spec?.auth) values.spec = { ...values.spec, auth: {} };
    if (!values.spec.auth.type) {
        res['spec.auth.type'] = 'ra.validation.required';
    } else if (
        values.spec.auth.type === 'basicAuth' ||
        values.spec.auth.type === 'apiKey'
    ) {
        if (!values.spec.auth.secretName) {
            res['spec.auth.secretName'] = 'ra.validation.required';
        }
    } else if (values.spec.auth.type === 'jwt') {
        if (!values.spec.auth.issuer)
            res['spec.auth.issuer'] = 'ra.validation.required';
        if (!values.spec.auth.jwksUri)
            res['spec.auth.jwksUri'] = 'ra.validation.required';
    }
    return res;
};

/********************** Data helpers **********************/

/**
 * Build the SecurityPolicy CR body for the given HTTPRoute name and auth config.
 * Returns null when auth type is 'none'.
 */
const buildSecurityPolicyData = (routeName: string, auth: any) => {
    const base = {
        apiVersion: SP_API_VERSION,
        kind: SP_KIND,
        metadata: { name: `${routeName}-sp` },
        spec: {
            targetRefs: [
                {
                    group: 'gateway.networking.k8s.io',
                    kind: 'HTTPRoute',
                    name: routeName,
                },
            ],
        } as any,
    };

    if (auth.type === 'basicAuth') {
        base.spec.basicAuth = { users: { name: auth.secretName } };
        return base;
    }
    if (auth.type === 'apiKey') {
        base.spec.apiKeyAuth = {
            credentialRefs: [{ name: auth.secretName }],
            extractFrom: [{ headers: [auth.headerName || 'x-api-key'] }],
        };
        return base;
    }
    if (auth.type === 'jwt') {
        const audiences = (auth.audiences || [])
            .map((a: any) => a.value)
            .filter(Boolean);
        const claimToHeaders = (auth.claimToHeaders || []).filter(
            (c: any) => c?.claim && c?.header
        );
        const provider: any = {
            name: 'default',
            issuer: auth.issuer,
            remoteJWKS: { uri: auth.jwksUri },
        };
        if (audiences.length > 0) provider.audiences = audiences;
        if (claimToHeaders.length > 0) provider.claimToHeaders = claimToHeaders;
        base.spec.jwt = { providers: [provider] };
        return base;
    }
    return null;
};

/** Convert flat form data to the actual HTTPRoute spec. */
const buildHttpRouteSpec = (data: any) => {
    const rule: any = {
        backendRefs: [
            {
                name: data.spec.backendService,
                port: parseInt(data.spec.backendPort, 10),
            },
        ],
    };
    if (data.spec.path) {
        rule.matches = [{ path: { type: 'PathPrefix', value: data.spec.path } }];
    }
    if (data.spec.backendPath) {
        rule.filters = [
            {
                type: 'URLRewrite',
                urlRewrite: {
                    path: {
                        type: 'ReplacePrefixMatch',
                        replacePrefixMatch: data.spec.backendPath,
                    },
                },
            },
        ];
    }
    return {
        hostnames: [data.spec.hostname],
        rules: [rule],
    };
};

/********************** Shared form **********************/

const BackendPortInput = () => {
    const service = useWatch({ name: 'spec.backendService' });
    const { data } = useGetOne('k8s_service', { id: service }, { enabled: !!service });
    const ports: { id: number; name: string }[] =
        data?.spec?.ports?.map((p: any) => ({
            id: p.port,
            name: p.name ? `${p.port} (${p.name})` : String(p.port),
        })) ?? [];
    return (
        <SelectInput
            fullWidth
            source="spec.backendPort"
            choices={ports}
            validate={required()}
        />
    );
};

const ServiceOptionRenderer = () => {
    const record = useRecordContext();
    const types = labels2types(record.metadata.labels);
    return (
        <Box sx={{ overflow: 'hidden' }}>
            <Box sx={{ whiteSpace: 'nowrap', textOverflow: 'ellipsis' }}>
                {record.metadata.name}
            </Box>
            {types ? (
                <ArrayField source="types" record={{ types }}>
                    <SingleFieldList linkType={false}>
                        <ChipField source="name" size="small" />
                    </SingleFieldList>
                </ArrayField>
            ) : (
                <></>
            )}
        </Box>
    );
};

const HTTPRouteForm = () => {
    const translate = useTranslate();
    const id = useWatch({ name: 'id' });

    return (
        <>
            <Grid container spacing={2}>
                <Grid item xs={6}>
                    <TextInput
                        disabled={!!id}
                        fullWidth
                        source="metadata.name"
                        validate={required()}
                    />
                </Grid>
                <Grid item xs={6}>
                    <TextInput
                        fullWidth
                        source="spec.hostname"
                        validate={required()}
                    />
                </Grid>
                <Grid item xs={8}>
                    <ReferenceInput
                        reference="k8s_service"
                        perPage={1000}
                        source="spec.backendService"
                    >
                        <AutocompleteInput
                            fullWidth
                            optionText={<ServiceOptionRenderer />}
                            inputText={(choice: any) => choice.metadata.name}
                            matchSuggestion={(filter: string, choice: any) =>
                                choice.metadata.name
                                    .toLowerCase()
                                    .includes(filter.toLowerCase())
                            }
                            validate={required()}
                        />
                    </ReferenceInput>
                </Grid>
                <Grid item xs={4}>
                    <FormDataConsumer>
                        {({ formData }) =>
                            formData.spec?.backendService ? (
                                <BackendPortInput />
                            ) : (
                                <SelectInput
                                    fullWidth
                                    source="spec.backendPort"
                                    choices={[]}
                                    disabled
                                    validate={required()}
                                />
                            )
                        }
                    </FormDataConsumer>
                </Grid>
                <Grid item xs={6}>
                    <TextInput
                        fullWidth
                        source="spec.path"
                        placeholder="/"
                    />
                </Grid>
                <Grid item xs={6}>
                    <TextInput
                        fullWidth
                        source="spec.backendPath"
                        placeholder="/"
                    />
                </Grid>
            </Grid>

            <Typography variant="h6" sx={{ paddingTop: '20px' }}>
                {translate(`resources.${CR_HTTPROUTES}.authenticationTitle`)}
            </Typography>

            <Grid container spacing={2}>
                <Grid item xs={3}>
                    <SelectInput
                        fullWidth
                        source="spec.auth.type"
                        choices={[
                            { id: 'none', name: 'None' },
                            { id: 'basicAuth', name: 'BasicAuth' },
                            { id: 'apiKey', name: 'ApiKey' },
                            { id: 'jwt', name: 'JWT' },
                        ]}
                        validate={required()}
                    />
                </Grid>
                <Grid item xs={9}>
                    <FormDataConsumer>
                        {({ formData }) => (
                            <>
                                {(formData.spec?.auth?.type === 'basicAuth' ||
                                    formData.spec?.auth?.type === 'apiKey') && (
                                    <Grid container spacing={2}>
                                        <Grid item xs={6}>
                                            <TextInput
                                                fullWidth
                                                source="spec.auth.secretName"
                                                validate={required()}
                                            />
                                        </Grid>
                                        {formData.spec?.auth?.type ===
                                            'apiKey' && (
                                            <Grid item xs={6}>
                                                <TextInput
                                                    fullWidth
                                                    source="spec.auth.headerName"
                                                />
                                            </Grid>
                                        )}
                                    </Grid>
                                )}
                            </>
                        )}
                    </FormDataConsumer>
                </Grid>
                <Grid item xs={12}>
                    <FormDataConsumer>
                        {({ formData }) =>
                            formData.spec?.auth?.type === 'jwt' ? (
                                <Grid container spacing={2}>
                                    <Grid item xs={6}>
                                        <TextInput
                                            fullWidth
                                            source="spec.auth.issuer"
                                            validate={required()}
                                        />
                                    </Grid>
                                    <Grid item xs={6}>
                                        <TextInput
                                            fullWidth
                                            source="spec.auth.jwksUri"
                                            validate={required()}
                                        />
                                    </Grid>
                                    <Grid item xs={12}>
                                        <ArrayInput source="spec.auth.audiences">
                                            <SimpleFormIterator inline>
                                                <TextInput source="value" />
                                            </SimpleFormIterator>
                                        </ArrayInput>
                                    </Grid>
                                    <Grid item xs={12}>
                                        <ArrayInput source="spec.auth.claimToHeaders">
                                            <SimpleFormIterator inline>
                                                <TextInput
                                                    source="claim"
                                                    validate={required()}
                                                />
                                                <TextInput
                                                    source="header"
                                                    validate={required()}
                                                />
                                            </SimpleFormIterator>
                                        </ArrayInput>
                                    </Grid>
                                </Grid>
                            ) : null
                        }
                    </FormDataConsumer>
                </Grid>
            </Grid>
        </>
    );
};

/********************** CRUD components **********************/

const CrCreate = () => {
    const { apiVersion, kind } = useCrTransform();
    const redirect = useRedirect();
    const [createSecPolicy] = useCreate();
    const authRef = useRef<any>({ type: 'none' });

    const defaults = () => ({
        spec: { auth: { type: 'none', headerName: 'x-api-key' } },
    });

    const transform = (data: any) => {
        authRef.current = data.spec?.auth || { type: 'none' };
        return {
            apiVersion,
            kind,
            metadata: data.metadata,
            spec: buildHttpRouteSpec(data),
        };
    };

    const onSuccess = (httpRoute: any) => {
        const auth = authRef.current;
        const routeName = httpRoute.metadata?.name || httpRoute.id;
        const spData = buildSecurityPolicyData(routeName, auth);
        if (spData) {
            createSecPolicy(CR_SECURITYPOLICIES, { data: spData });
        }
        redirect('list', CR_HTTPROUTES);
    };

    const validate = (values: any) => {
        if (!apiVersion || !kind) {
            return {
                apiVersion: 'ra.validation.required',
                kind: 'ra.validation.required',
            };
        }
        return validateFields(values);
    };

    return (
        <>
            <Breadcrumb />
            <SimplePageTitle pageType="create" crName={CR_HTTPROUTES} />
            <Create
                redirect={false}
                actions={<CreateTopToolbar />}
                transform={transform}
                mutationOptions={{ onSuccess }}
            >
                <SimpleForm validate={validate} defaultValues={defaults}>
                    <HTTPRouteForm />
                </SimpleForm>
            </Create>
        </>
    );
};

const CrEdit = () => {
    const { record } = useEditController();
    const { apiVersion, kind } = useCrTransform();
    const redirect = useRedirect();
    const [createSecPolicy] = useCreate();
    const [updateSecPolicy] = useUpdate();
    const [deleteSecPolicy] = useDelete();
    const authRef = useRef<any>({ type: 'none' });

    const routeName = record?.metadata?.name;
    const { data: secPolicy, isLoading: spLoading } = useGetOne(
        CR_SECURITYPOLICIES,
        { id: `${routeName}-sp` },
        { enabled: !!routeName, retry: false }
    );

    if (!record) return null;
    if (spLoading) return null;

    // Derive existing auth config from SecurityPolicy (if present)
    let authType = 'none';
    let authSecretName = '';
    let authHeaderName = 'x-api-key';
    let authJwtIssuer = '';
    let authJwtJwksUri = '';
    let authJwtAudiences: any[] = [];
    let authJwtClaimToHeaders: any[] = [];
    if (secPolicy?.spec?.basicAuth) {
        authType = 'basicAuth';
        authSecretName = secPolicy.spec.basicAuth.users?.name || '';
    } else if (secPolicy?.spec?.apiKeyAuth) {
        authType = 'apiKey';
        authSecretName =
            secPolicy.spec.apiKeyAuth.credentialRefs?.[0]?.name || '';
        authHeaderName =
            secPolicy.spec.apiKeyAuth.extractFrom?.[0]?.headers?.[0] || 'x-api-key';
    } else if (secPolicy?.spec?.jwt) {
        authType = 'jwt';
        const jwtProvider = secPolicy.spec.jwt.providers?.[0] || {};
        authJwtIssuer = jwtProvider.issuer || '';
        authJwtJwksUri = jwtProvider.remoteJWKS?.uri || '';
        authJwtAudiences = (jwtProvider.audiences || []).map((v: string) => ({ value: v }));
        authJwtClaimToHeaders = jwtProvider.claimToHeaders || [];
    }

    // Pre-populate form with flattened HTTPRoute spec + derived auth
    const defaultValues = {
        spec: {
            hostname: record.spec?.hostnames?.[0] || '',
            backendService:
                record.spec?.rules?.[0]?.backendRefs?.[0]?.name || '',
            backendPort:
                record.spec?.rules?.[0]?.backendRefs?.[0]?.port || '',
            path:
                record.spec?.rules?.[0]?.matches?.[0]?.path?.value || '',
            backendPath:
                record.spec?.rules?.[0]?.filters?.find(
                    (f: any) => f.type === 'URLRewrite'
                )?.urlRewrite?.path?.replacePrefixMatch || '',
            auth: {
                type: authType,
                secretName: authSecretName,
                headerName: authHeaderName,
                issuer: authJwtIssuer,
                jwksUri: authJwtJwksUri,
                audiences: authJwtAudiences,
                claimToHeaders: authJwtClaimToHeaders,
            },
        },
    };

    const transform = (data: any) => {
        authRef.current = data.spec?.auth || { type: 'none' };
        return {
            apiVersion,
            kind,
            metadata: data.metadata,
            spec: buildHttpRouteSpec(data),
        };
    };

    const onSuccess = (httpRoute: any) => {
        const auth = authRef.current;
        const rName = httpRoute.metadata?.name || httpRoute.id;
        const spId = `${rName}-sp`;

        if (auth.type === 'none') {
            if (secPolicy) {
                deleteSecPolicy(CR_SECURITYPOLICIES, {
                    id: spId,
                    previousData: secPolicy,
                });
            }
        } else {
            const spData = buildSecurityPolicyData(rName, auth);
            if (spData) {
                if (secPolicy) {
                    updateSecPolicy(
                        CR_SECURITYPOLICIES,
                        { id: spId, data: spData, previousData: secPolicy },
                        {
                            onError: () => {
                                createSecPolicy(CR_SECURITYPOLICIES, {
                                    data: spData,
                                });
                            },
                        }
                    );
                } else {
                    createSecPolicy(CR_SECURITYPOLICIES, { data: spData });
                }
            }
        }
        redirect('list', CR_HTTPROUTES);
    };

    const validate = (values: any) => {
        if (!apiVersion || !kind) {
            return {
                apiVersion: 'ra.validation.required',
                kind: 'ra.validation.required',
            };
        }
        return validateFields(values);
    };

    return (
        <>
            <Breadcrumb />
            <SimplePageTitle pageType="edit" crName={CR_HTTPROUTES} />
            <Edit
                redirect={false}
                actions={<EditTopToolbar />}
                transform={transform}
                mutationOptions={{ onSuccess }}
                mutationMode="pessimistic"
            >
                <SimpleForm validate={validate} defaultValues={defaultValues}>
                    <HTTPRouteForm />
                </SimpleForm>
            </Edit>
        </>
    );
};

const CrList = () => {
    const translate = useTranslate();
    const { permissions } = usePermissions();
    const hasPermission = (op: string) =>
        permissions && permissions.canAccess(CR_HTTPROUTES, op);

    return (
        <>
            <Breadcrumb />
            <SimplePageTitle pageType="list" crName={CR_HTTPROUTES} />
            <List
                actions={
                    <ListTopToolbar hasCreate={hasPermission('write')} />
                }
                hasCreate={hasPermission('write')}
            >
                <Datagrid>
                    <TextField source="id" />
                    <FunctionField
                        label={translate(
                            `resources.${CR_HTTPROUTES}.fields.spec.hostname`
                        )}
                        render={(record: any) =>
                            record.spec?.hostnames?.[0] || ''
                        }
                    />
                    <FunctionField
                        label={translate(
                            `resources.${CR_HTTPROUTES}.fields.spec.backendService`
                        )}
                        render={(record: any) =>
                            record.spec?.rules?.[0]?.backendRefs?.[0]?.name ||
                            ''
                        }
                    />
                    <FunctionField
                        label={translate(
                            `resources.${CR_HTTPROUTES}.fields.spec.backendPort`
                        )}
                        render={(record: any) =>
                            record.spec?.rules?.[0]?.backendRefs?.[0]?.port ||
                            ''
                        }
                    />
                    <Box textAlign="right">
                        {hasPermission('write') && <EditButton />}
                        {hasPermission('read') && <ShowButton />}
                        {hasPermission('write') && <DeleteWithConfirmButton />}
                    </Box>
                </Datagrid>
            </List>
        </>
    );
};

const CrShow = () => {
    const translate = useTranslate();
    const { record } = useShowController();
    const { permissions } = usePermissions();
    const hasPermission = (op: string) =>
        permissions && permissions.canAccess(CR_HTTPROUTES, op);

    const routeName = record?.metadata?.name;
    const { data: secPolicy } = useGetOne(
        CR_SECURITYPOLICIES,
        { id: `${routeName}-sp` },
        { enabled: !!routeName, retry: false }
    );

    if (!record) return null;

    let authType = 'None';
    if (secPolicy?.spec?.basicAuth) authType = 'BasicAuth';
    else if (secPolicy?.spec?.apiKeyAuth) authType = 'ApiKey';
    else if (secPolicy?.spec?.jwt) authType = 'JWT';

    return (
        <>
            <Breadcrumb />
            <SimplePageTitle pageType="show" crName={CR_HTTPROUTES} />
            <Show
                actions={
                    <ShowTopToolbar
                        hasYaml
                        hasEdit={hasPermission('write')}
                        hasDelete={hasPermission('write')}
                    />
                }
            >
                <SimpleShowLayout>
                    <FunctionField
                        label={translate(
                            `resources.${CR_HTTPROUTES}.fields.spec.hostname`
                        )}
                        render={(r: any) => r.spec?.hostnames?.[0] || ''}
                    />
                    <FunctionField
                        label={translate(
                            `resources.${CR_HTTPROUTES}.fields.spec.backendService`
                        )}
                        render={(r: any) =>
                            r.spec?.rules?.[0]?.backendRefs?.[0]?.name || ''
                        }
                    />
                    <FunctionField
                        label={translate(
                            `resources.${CR_HTTPROUTES}.fields.spec.backendPort`
                        )}
                        render={(r: any) =>
                            r.spec?.rules?.[0]?.backendRefs?.[0]?.port || ''
                        }
                    />
                    <FunctionField
                        label={translate(
                            `resources.${CR_HTTPROUTES}.fields.spec.path`
                        )}
                        render={(r: any) =>
                            r.spec?.rules?.[0]?.matches?.[0]?.path?.value || ''
                        }
                    />
                    <FunctionField
                        label={translate(
                            `resources.${CR_HTTPROUTES}.fields.spec.backendPath`
                        )}
                        render={(r: any) =>
                            r.spec?.rules?.[0]?.filters?.find(
                                (f: any) => f.type === 'URLRewrite'
                            )?.urlRewrite?.path?.replacePrefixMatch || ''
                        }
                    />
                    <FunctionField
                        label={translate(
                            `resources.${CR_HTTPROUTES}.fields.spec.gatewayName`
                        )}
                        render={(r: any) =>
                            r.spec?.parentRefs?.[0]?.name || ''
                        }
                    />
                    <Typography variant="h6" sx={{ paddingTop: '20px' }}>
                        {translate(
                            `resources.${CR_HTTPROUTES}.authenticationTitle`
                        )}
                    </Typography>
                    <Labeled
                        label={translate(
                            `resources.${CR_HTTPROUTES}.fields.spec.auth.type`
                        )}
                    >
                        <Box>{authType}</Box>
                    </Labeled>
                    {secPolicy?.spec?.basicAuth && (
                        <Labeled
                            label={translate(
                                `resources.${CR_HTTPROUTES}.fields.spec.auth.secretName`
                            )}
                        >
                            <Box>
                                {secPolicy.spec.basicAuth.users?.name || ''}
                            </Box>
                        </Labeled>
                    )}
                    {secPolicy?.spec?.apiKeyAuth && (
                        <>
                            <Labeled
                                label={translate(
                                    `resources.${CR_HTTPROUTES}.fields.spec.auth.secretName`
                                )}
                            >
                                <Box>
                                    {secPolicy.spec.apiKeyAuth.credentialRefs?.[0]
                                        ?.name || ''}
                                </Box>
                            </Labeled>
                            <Labeled
                                label={translate(
                                    `resources.${CR_HTTPROUTES}.fields.spec.auth.headerName`
                                )}
                            >
                                <Box>
                                    {secPolicy.spec.apiKeyAuth.extractFrom?.[0]
                                        ?.headers?.[0] || ''}
                                </Box>
                            </Labeled>
                        </>
                    )}
                    {secPolicy?.spec?.jwt && (() => {
                        const provider = secPolicy.spec.jwt.providers?.[0] || {};
                        return (
                            <>
                                <Labeled
                                    label={translate(
                                        `resources.${CR_HTTPROUTES}.fields.spec.auth.issuer`
                                    )}
                                >
                                    <Box>{provider.issuer || ''}</Box>
                                </Labeled>
                                <Labeled
                                    label={translate(
                                        `resources.${CR_HTTPROUTES}.fields.spec.auth.jwksUri`
                                    )}
                                >
                                    <Box>{provider.remoteJWKS?.uri || ''}</Box>
                                </Labeled>
                                {(provider.audiences || []).length > 0 && (
                                    <Labeled
                                        label={translate(
                                            `resources.${CR_HTTPROUTES}.fields.spec.auth.audiences`
                                        )}
                                    >
                                        <Box>
                                            {(provider.audiences || []).join(', ')}
                                        </Box>
                                    </Labeled>
                                )}
                                {(provider.claimToHeaders || []).map(
                                    (c: any, i: number) => (
                                        <Labeled
                                            key={i}
                                            label={translate(
                                                `resources.${CR_HTTPROUTES}.fields.spec.auth.claimToHeader`,
                                                { claim: c.claim }
                                            )}
                                        >
                                            <Box>{c.header}</Box>
                                        </Labeled>
                                    )
                                )}
                            </>
                        );
                    })()}
                </SimpleShowLayout>
            </Show>
        </>
    );
};

const CustomView: View = {
    key: CR_HTTPROUTES,
    name: 'HTTP Routes',
    list: CrList,
    show: CrShow,
    create: CrCreate,
    edit: CrEdit,
    icon: AltRouteIcon,
};

const GWPolicyView: View = {
    key: CR_SECURITYPOLICIES,
    name: 'Security Policies'
};

export default CustomView;

export { GWPolicyView };
