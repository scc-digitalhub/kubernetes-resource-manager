// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

import {
    createContext,
    useContext,
    useEffect,
    useState,
    ReactNode,
} from 'react';
import { useDataProvider } from 'react-admin';

/** Shape of the backend config map returned by GET /api/config. */
export type BackendConfig = Record<string, string>;

const BackendConfigContext = createContext<BackendConfig>({});

/**
 * Fetches GET /api/config once on mount (after authentication) and makes
 * the resulting key→value map available to all descendants via context.
 */
export const BackendConfigProvider = ({ children }: { children: ReactNode }) => {
    const [config, setConfig] = useState<BackendConfig>({});
    const dataProvider = useDataProvider();

    useEffect(() => {
        (dataProvider as any)
            .fetchConfig()
            .then((cfg: BackendConfig) => setConfig(cfg))
            .catch(() => {
                // config endpoint unavailable – keep empty map
            });
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <BackendConfigContext.Provider value={config}>
            {children}
        </BackendConfigContext.Provider>
    );
};

/** Returns the backend config map. Values are empty string when not set. */
export const useBackendConfig = (): BackendConfig =>
    useContext(BackendConfigContext);
