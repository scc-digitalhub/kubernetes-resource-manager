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
    ReferenceInput,
    AutocompleteInput,
    required,
    FormDataConsumer,
    DeleteWithConfirmButton,
    useGetOne,
    Loading,
    useNotify,
    useRedirect,
    useResourceContext,
} from 'react-admin';
import { CrdProps } from '../components/CrdProps';
import { DeleteConfirmToolbar } from '../components/DeleteConfirmToolbar';
import { useUpdateCrdIds } from '../hooks/useUpdateCrdIds';

export const SchemaList = () => {
    const notify = useNotify();

    const { updateCrdIds } = useUpdateCrdIds();

    const onSuccess = (data: any) => {
        updateCrdIds();
        notify('ra.notification.deleted', { messageArgs: { smart_count: 1 } });
    };
    return (
        <List>
            <Datagrid bulkActionButtons={false}>
                <TextField source="id" />
                <TextField source="crdId" label="CRD" />
                <TextField source="version" />
                <TextField source="schema" />
                <EditButton />
                <ShowButton />
                <DeleteWithConfirmButton mutationOptions={{ onSuccess }} />
            </Datagrid>
        </List>
    );
};

export const SchemaEdit = () => (
    <Edit>
        <SimpleForm toolbar={<DeleteConfirmToolbar />}>
            <TextInput source="id" disabled sx={{ width: '22em' }} />
            <TextInput
                source="crdId"
                label="CRD"
                disabled
                sx={{ width: '22em' }}
            />
            <TextInput source="version" disabled />
            <TextInput source="schema" fullWidth />
        </SimpleForm>
    </Edit>
);

export const SchemaCreate = () => {
    const notify = useNotify();
    const redirect = useRedirect();
    const resource = useResourceContext();
    const { updateCrdIds } = useUpdateCrdIds();

    const onSuccess = (data: any) => {
        updateCrdIds();
        notify('ra.notification.created', { messageArgs: { smart_count: 1 } });
        redirect('edit', resource, data.id, data);
    };

    const params = new URLSearchParams(window.location.search);
    const crdIdFromQuery = params.get('crdId');

    return (
        <Create mutationOptions={{ onSuccess }}>
            <SimpleForm>
                <ReferenceInput source="crdId" reference="crd">
                    <AutocompleteInput
                        label="CRD"
                        validate={required()}
                        sx={{ width: '22em' }}
                        defaultValue={
                            crdIdFromQuery ? crdIdFromQuery : undefined
                        }
                    />
                </ReferenceInput>
                <FormDataConsumer>
                    {({ formData, ...rest }) =>
                        formData.crdId ? (
                            <SchemaVersionInput
                                crdId={formData.crdId}
                                {...rest}
                            />
                        ) : (
                            <TextInput
                                source="version"
                                helperText="Please select a CRD"
                                disabled
                            />
                        )
                    }
                </FormDataConsumer>
                <TextInput source="schema" fullWidth />
            </SimpleForm>
        </Create>
    );
};

export const SchemaShow = () => (
    //TODO aggiungere collegamento per cliccare sul crd id e aprire la crd
    <Show>
        <SimpleShowLayout>
            <TextField source="id" />
            <TextField source="crdId" label="CRD" />
            <TextField source="version" />
            <TextField source="schema" />
        </SimpleShowLayout>
    </Show>
);

const SchemaVersionInput = ({ crdId }: CrdProps) => {
    const { data, isLoading } = useGetOne('crd', { id: crdId });
    if (isLoading) return <Loading />;
    if (!data) return null;
    const storedVersion = data.spec.versions.filter(
        (version: any) => version.storage
    )[0];
    return (
        <TextInput
            source="version"
            defaultValue={storedVersion.name}
            disabled
        />
    );
};
