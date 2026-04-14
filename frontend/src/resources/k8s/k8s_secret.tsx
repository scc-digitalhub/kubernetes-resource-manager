// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

import {
    Datagrid,
    List,
    Show,
    ShowButton,
    SimpleShowLayout,
    TextField,
    ArrayField,
    useShowController,
    useTranslate,
    useRecordContext,
    Button,
    useDataProvider,
    Create,
    SimpleForm,
    TextInput,
    required,
    ArrayInput,
    SimpleFormIterator,
    useNotify,
    usePermissions,
    DeleteWithConfirmButton,
} from 'react-admin';
import { Box, Grid, Typography } from '@mui/material';
import { Breadcrumb } from '@dslab/ra-breadcrumb';
import { ContentCopy } from '@mui/icons-material';
import { CreateTopToolbar, ListTopToolbar, ShowTopToolbar } from '../../components/toolbars';



export const K8SSecretCreate = () => {
    const translate = useTranslate();

    const validate = (values: any) => {
        const errors : any  = {};
        if (!values.name) {
            errors.name = 'ra.validation.required';
        }
        return errors;
    };

    const transform = (values: any) => {
        const data: any = {};
        if (values.data) {
            values.data.forEach((e: any) => {
                if (e.name) {
                    data[e.name] = e.value;
                }
            });
        }
        values.data = data;            
        return values;
    }

    return (
        <>
            <Breadcrumb />
            <Typography variant="h4" className="page-title">
                {translate('ra.page.create', {
                    name: translate('resources.k8s_secret.name', {
                        smart_count: 1,
                    }).toLowerCase(),
                })}
            </Typography>
            <Create
                redirect="list"
                actions={<CreateTopToolbar />}
                transform={transform}
            >
                <SimpleForm noValidate validate={validate}>
                    <Grid container spacing={2}>
                        <Grid item xs={4}>
                            <TextInput
                                fullWidth
                                source="name"
                                validate={required()}
                                label={'resources.k8s_secret.fields.metadata.name'}
                            />
                        </Grid>
                        <Grid item xs={12}>
                            <ArrayInput source="data">
                            <SimpleFormIterator inline>
                                <TextInput validate={required()} source="name" helperText={false} />
                                <TextInput source="value" helperText={false} />
                            </SimpleFormIterator>
                            </ArrayInput>
                        </Grid>
                    </Grid>
                </SimpleForm>
            </Create>
        </>
    );
};

const DataNumField = (props: any) => {
    const record = useRecordContext(props);
    return (<TextField source="num" record={{
        num: record.data ? Object.keys(record.data).length : 0
    }} />)
};
    
    
export const K8SSecretList = () => {
    const { permissions } = usePermissions();
    const hasPermission = (op: string) => permissions && permissions.canAccess('k8s_secret', op)

return <>
        <Breadcrumb />
        <List actions={<ListTopToolbar hasCreate={hasPermission('write')}/>}>
            <Datagrid bulkActionButtons={false}>
                <TextField source="metadata.name" />
                <TextField source="type" />
                <DataNumField label="resources.k8s_secret.fields.secretnum" />
                <Box textAlign={'right'}>
                    {hasPermission('read') && <ShowButton />}
                    {hasPermission('write') && <DeleteWithConfirmButton />}
                </Box>
            </Datagrid>
        </List>
    </>
};

const DecodeButton = (props: any) => {
    const record = useRecordContext(props);
    const provider = useDataProvider();
    const notify = useNotify();
    
    const decode = () => {
        console.log('Decoding', record, props);
        provider.decodeSecret(props.secret, record.name)
        .then((value: string) => {
            navigator.clipboard.writeText(value[record.name]);
            notify('clipboard.copied', { type: 'info' });
        })
        .catch((err: any) => console.log(err));
    };

    return (
        <Button label='resources.k8s_secret.decode' startIcon={ <ContentCopy /> } onClick={decode}></Button>
    )
};

export const K8SSecretShow = () => {
    const translate = useTranslate();
    const { record } = useShowController();
    const { permissions } = usePermissions();
    const hasPermission = (op: string) => permissions && permissions.canAccess('k8s_secret', op)
    if (!record) return null;
    
    return (
        <>
            <Breadcrumb />
            <Typography variant="h4" className="page-title">
                {translate('ra.page.show', {
                    name: 'Secret',
                    recordRepresentation: record.id,
                })}
            </Typography>
            <Show actions={<ShowTopToolbar hasYaml hasEdit={false} hasDelete={hasPermission('write')} />}>
                <SimpleShowLayout>
                    <TextField source="metadata.name" />
                    <TextField source="type" />
                    <ArrayField source="data" record={{
                        data: Object.keys(record.data || {}).map((t: any) => ({name: t})),
                    }}>
                        <Datagrid bulkActionButtons={false}>
                            <TextField label="resources.k8s_secret.fields.secretname" source="name" />
                            <DecodeButton secret={record.metadata.name}/>
                        </Datagrid>
                    </ArrayField>
                </SimpleShowLayout>
            </Show>
        </>
    );
};

