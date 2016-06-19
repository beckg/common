//-----------------------------------------------------------------------------------
// Copyright (c) 2009, Aventinus Ltd (aventinus.org). All rights reserved.
//-----------------------------------------------------------------------------------
#ifndef JSONMAP_INCL
#define JSONMAP_INCL

#include "TextString.h"
#include "Map.h"

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
class JSONObject;
class JSONArray;

class JSONMap
{
    friend class JSONObject;

    public:
        JSONMap();
        virtual ~JSONMap();
        JSONMap(const JSONMap& other);
        JSONMap& operator=(const JSONMap& other);

        JSONMap& put(const TextString& name, const TextString& value);
        JSONMap& put(const TextString& name, int value);
        JSONMap& put(const TextString& name, long value);
        JSONMap& put(const TextString& name, double value);
        JSONMap& put(const TextString& name, const JSONObject& value);
        JSONMap& put(const TextString& name, const JSONMap& value);

        TextString toString() const;

        int size() const;
        int isObject(const TextString& name) const;
        int isArray(const TextString& name) const;
        int isValue(const TextString& name) const;
        int contains(const TextString& name) const;

        const TextString& getName(int index) const;

        TextString getString(const TextString& name) const;
        TextString getString(const TextString& name, const TextString& defaultValue) const;
        int getInteger(const TextString& name, int& value) const;
        int getInteger(const TextString& name, int& value, int defaultValue) const;
        int getLong(const TextString& name, long& value) const;
        int getLong(const TextString& name, long& value, long defaultValue) const;
        int getDouble(const TextString& name, double& value) const;
        int getDouble(const TextString& name, double& value, double defaultValue) const;
        JSONObject& getObject(const TextString& name, JSONObject& target) const;
        JSONArray& getArray(const TextString& name, JSONArray& target) const;
        JSONMap& getMap(const TextString& name, JSONMap& target) const;

        const TextString& toString(int index) const;

        int hasError() const                                   {return (m_error.length() > 0);}
        const TextString& getError() const                     {return m_error;}

        void dump() const;
        void dump(const TextString& text) const;
        void dump(TextString buffer, int level) const;

    protected:
        void putRaw(const TextString& name, const TextString& value);
        int getRaw(const TextString& name, TextString& target) const;

        TextString m_error;

    private:
        Map<TextString, TextString> m_map;
};
#endif
