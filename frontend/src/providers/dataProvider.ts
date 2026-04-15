// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

import { stringify } from 'querystring';
import { fetchUtils, DataProvider } from 'ra-core';
import jsonServerProvider from 'ra-data-json-server';

export const dataProvider = (
    baseUrl: string,
    httpClient: (
        url: any,
        options?: fetchUtils.Options | undefined
    ) => Promise<{
        status: number;
        headers: Headers;
        body: string;
        json: any;
    }>
): DataProvider => {
    const apiUrl = baseUrl + '/api';
    const provider = jsonServerProvider(apiUrl, httpClient);

    return {
        fetchResources: async (): Promise<string[]> => {
            return httpClient(`${apiUrl}/crs?all=true&size=1000`).then(
                ({ headers, json }) => {
                    if (!json.content) {
                        throw new Error('the response must match page<> model');
                    }
                    const crdIds: string[] = json.content.map(
                        (crs: any) => crs.crdId
                    );
                    return Array.from(new Set(crdIds)).sort(
                        (a: string, b: string) => a.localeCompare(b)
                    );
                }
            );
        },
        getList: (resource, params) => {
            const { page, perPage } = params.pagination;
            const { field, order } = params.sort;
            const query = {
                sort: field + ',' + order,
                page: page - 1,
                size: perPage,
            };
            let url = `${apiUrl}/${resource}?${stringify(query)}`;

            const idFilter = params?.filter?.id;
            if (idFilter) {
                url += `&id=${idFilter}`;
            }

            const onlyWithoutSchema = params?.filter?.onlyWithoutSchema;
            if (onlyWithoutSchema) {
                url += `&onlyWithoutSchema=${onlyWithoutSchema}`;
            }

            return httpClient(url).then(({ headers, json }) => {
                if (!json.content) {
                    throw new Error('the response must match page<> model');
                }
                return {
                    data: json.content,
                    total: parseInt(json.totalElements, 10),
                };
            });
        },
        getOne: (resource, params) => {
            let headers = {};
            if (params?.meta?.yaml) {
                headers = new Headers({ Accept: 'application/x-yaml' });
            }
            return httpClient(`${apiUrl}/${resource}/${params.id}${params?.meta?.log ? '/log' : ''}`, {
                headers: headers,
            }).then(({ json, body }) => {
                if (params?.meta?.yaml) {
                    return { data: { id: 'yamltext', yaml: body } };
                }
                if (params?.meta?.log) {
                    return { data: { id: 'log', records: json } };
                }
                return {
                    data: json,
                };
            });
        },
        getMany: (resource, params) => {
            let url = `${apiUrl}/${resource}`;
            const ids = params.ids;
            if (ids && ids.length > 0) {
                url += `?id=${ids.join(',')}`;
            }

            return httpClient(url).then(({ headers, json }) => {
                if (!json.content) {
                    throw new Error('the response must match page<> model');
                }
                return {
                    data: json.content,
                    total: parseInt(json.totalElements, 10),
                };
            });
        },
        getManyReference: (resource, params) => {
            const { page, perPage } = params.pagination;
            const { field, order } = params.sort;
            const query = {
                sort: field + ',' + order,
                page: page - 1,
                size: perPage,
            };

            let url = `${apiUrl}`;
            if (resource === 'crs') {
                url += `/crd/${params.id}/schemas`;
            } else {
                url += `/${resource}`;
            }

            url += `?${stringify(query)}`;

            return httpClient(url).then(({ headers, json }) => {
                if (!json.content) {
                    throw new Error('the response must match page<> model');
                }
                return {
                    data: json.content,
                    total: parseInt(json.totalElements, 10),
                };
            });
        },
        update: (resource, params) => {
            let headers = {};
            if (params?.meta?.yaml) {
                headers = new Headers({ 'Content-Type': 'application/x-yaml' });
            }
            return httpClient(`${apiUrl}/${resource}/${params.id}`, {
                method: 'PUT',
                headers: headers,
                body:
                    typeof params.data === 'string'
                        ? params.data
                        : JSON.stringify(params.data),
            }).then(({ json }) => ({ data: json }));
        },
        updateMany: (resource, params) => provider.updateMany(resource, params),
        create: (resource, params) => provider.create(resource, params),
        delete: (resource, params) => provider.delete(resource, params),
        deleteMany: (resource, params) =>
            Promise.all(
                params.ids.map(id =>
                    httpClient(`${apiUrl}/${resource}/${id}`, {
                        method: 'DELETE',
                    })
                )
            ).then(responses => ({ data: responses.map(({ json }) => json) })),
        // secret-specific
        decodeSecret: (secret: string, key: string) => {
            return httpClient(`${apiUrl}/k8s_secret/${secret}/decode/${key}`).then(({ headers, json }) => {
                if (!json) {
                    throw new Error('the response is invalid');
                }
                return json;
            });
        },
        // backend config
        fetchConfig: (): Promise<Record<string, string>> => {
            return httpClient(`${apiUrl}/config`).then(({ json }) => {
                if (!json || typeof json !== 'object') {
                    return {};
                }
                return json as Record<string, string>;
            });
        },
    };
};

export default dataProvider;
