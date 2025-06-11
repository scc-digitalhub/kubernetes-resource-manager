// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

import { fetchUtils } from 'ra-core';
import { AuthProvider } from './authProvider';

export const httpClientProvider = (
    authProvider: AuthProvider
): ((
    url: any,
    options?: fetchUtils.Options | undefined
) => Promise<{
    status: number;
    headers: Headers;
    body: string;
    json: any;
}>) => {
    return async (url: string, options: fetchUtils.Options = {}) => {
        if (!options.headers) {
            options.headers = new Headers({ Accept: 'application/json' });
        } else {
            options.headers = new Headers(options.headers);
        }

        const authHeader = await authProvider.getAuthorization();
        if (authHeader) {
            options.headers.set('Authorization', authHeader);
        }

        if (!options.headers.has('Accept')) {
            options.headers.set('Accept', 'application/json');
        }

        return fetchUtils.fetchJson(url, options);
    };
};
