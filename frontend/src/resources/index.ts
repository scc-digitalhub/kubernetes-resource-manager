// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

import { ComponentType, ReactElement, createContext } from 'react';
import { CrCreate, CrEdit, CrList, CrShow } from './cr';

export interface View {
    key: string;
    name?: string;
    list?: ComponentType<any> | ReactElement;
    create?: ComponentType<any> | ReactElement;
    edit?: ComponentType<any> | ReactElement;
    show?: ComponentType<any> | ReactElement;
    icon?: ComponentType<any>;
}

export class Views {
    views: Array<View> = [];

    put(v: View) {
        this.views.push(v);
    }

    get(key: string) {
        return this.views.find(v => v.key === key);
    }

    list() {
        return this.views;
    }
}

export const fetchViews = (types: string[]): View[] => {
    return types.map(t => {
        let v = views.get(t);
        if (!v) {
            v = {
                key: t,
                name: t,
                list: CrList,
                create: CrCreate,
                edit: CrEdit,
                show: CrShow,
            };
        }
        return v;
    });
};

const views = new Views();
export const ViewsContext = createContext(views);
