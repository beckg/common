//-----------------------------------------------------------------------------------
// Copyright (c) 2009, Aventinus Ltd (aventinus.org). All rights reserved.
//-----------------------------------------------------------------------------------
#ifndef JSONARRAY_INCL
#define JSONARRAY_INCL

#include "TextString.h"
#include "List.h"

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
class JSONObject;

class JSONArray
{
    friend class JSONObject;
    friend class JSONMap;

    public:
        JSONArray();
        virtual ~JSONArray();
        JSONArray(const JSONArray& other);
        JSONArray& operator=(const JSONArray& other);

        JSONArray& add(const TextString& value);
        JSONArray& add(int value);
        JSONArray& add(long value);
        JSONArray& add(double value);
        JSONArray& add(const JSONObject& value);
        JSONArray& add(const JSONArray& value);

        TextString toString() const;

        int size() const;
        int isObject(int index) const;
        int isArray(int index) const;
        int isValue(int index) const;
        TextString getString(int index) const;
        int getInteger(int index, int& value) const;
        int getLong(int index, long& value) const;
        int getDouble(int index, double& value) const;
        JSONObject& getObject(int index, JSONObject& target) const;
        JSONArray& getArray(int index, JSONArray& target) const;

        const TextString& toString(int index) const;
        void normalise(JSONArray& target) const;

        int hasError() const                                   {return (m_error.length() > 0);}
        const TextString& getError() const                     {return m_error;}

        void dump() const;
        void dump(const TextString& text) const;
        void dump(TextString buffer, int level) const;

    protected:
        void addRaw(const TextString& value);

        TextString m_error;

    private:

        List<TextString> m_list;
};
#endif
