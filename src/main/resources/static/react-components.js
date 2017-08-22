var Content = React.createClass({
    fetchData: function(state, instance) {
        that = this;
        function getRestUrl(server, resource, params) {
            url = server + "/" + resource;
            if (Object.values(params).length > 0) {
                url += "?";
                for (key in params) {
                    url += key + "=" + params[key] + "&";
                }
            }
            return url;
        }
        this.setState({
            loading: true
        });
        var xhttp = new XMLHttpRequest();
        xhttp.onload = function() {
            var responseObject = JSON.parse(xhttp.responseText);
            that.setState({
                data: responseObject.content,
                pages: responseObject.totalPages,
                loading: false
            });
        }.bind(this);
        var params = {
            page: state.page,
            pagesize: state.pageSize
        };
        if (state.sorted.length > 0) {
            params.sortcol = state.sorted[0].id;
            params.sortdir = state.sorted[0].desc ? "desc" : "asc";
        } else {
            params.sortcol = "time";
            params.sortdir = "desc";
        }
        xhttp.open("GET", getRestUrl("https://yalp-store-crash-reports.duckdns.org", "crashreport", params), true);
        xhttp.send();
    },
    getStaticLink: function(row, type) {
        switch (type) {
            case "stacktrace":
                return "";
        }
    },
    render: function() {
        that = this;
        if (this.state == null) {
            this.state = {
                data: [],
                pages: -1,
                loading: true
            }
        }
        return (
            React.createElement(
                'div',
                null,
                React.createElement('h1', null, document.title),
                React.createElement(window.ReactTable.default, {
                    columns: [
                        {
                            Header: "Time",
                            id: 'time',
                            accessor: function (row) {
                                var d = new Date(row.time * 1000)
                                function pad(integer) {
                                    return integer > 9 ? integer : "0" + integer;
                                }
                                return d.getFullYear() + "." + pad(d.getMonth() + 1) + "." + pad(d.getDate()) + " " + pad(d.getHours()) + ":" + pad(d.getMinutes())
                            },
                            maxWidth: 150
                        },
                        {
                            Header: "vc",
                            accessor: "versionCode",
                            maxWidth: 50
                        },
                        {
                            Header: "Source",
                            accessor: "source",
                            maxWidth: 100
                        },
                        {
                            Header: "Device Name",
                            accessor: "deviceName",
                            maxWidth: 200
                        },
                        {
                            Header: "Files",
                            id: 'hasStackTrace',
                            accessor: function (row) {
                                return React.createElement(
                                    'span',
                                    {className:"files"},
                                    row.hasStackTrace ? React.createElement("a", {target: "_blank", href: "raw/" + row.directoryName + "/stacktrace.txt"}, "Stack Trace") : "",
                                    row.hasDeviceDefinition ? React.createElement("a", {target: "_blank", href: "raw/" + row.directoryName + "/device-" + row.deviceName + ".properties"}, "Device") : "",
                                    row.hasLog ? React.createElement("a", {target: "_blank", href: "raw/" + row.directoryName + "/log.txt"}, "Log") : "",
                                    row.hasPreferences ? React.createElement("a", {target: "_blank", href: "raw/" + row.directoryName + "/preferences.txt"}, "Preferences") : ""
                                );
                            },
                            minWidth: 250,
                            maxWidth: 350
                        },
                        {
                            Header: "User Name",
                            accessor: "userId",
                            maxWidth: 300
                        },
                        {
                            Header: "Message",
                            accessor: "message",
                            minWidth: 500
                        }
                    ],
                    onFetchData(state, instance) {
                        that.fetchData(state, instance);
                    },
                    data: this.state.data,
                    pages: this.state.pages,
                    loading: this.state.loading,
                    defaultPageSize: 20,
                    defaultSortDesc: true,
                    className: "-striped -highlight",
                    manual: true,
                    //filterable: true,
                    showPaginationTop: true
                })
            )
        )
    }
})
ReactDOM.render(React.createElement(Content), document.getElementById('content'))
if ('serviceWorker' in navigator) {
    window.addEventListener('load', function() {
        navigator.serviceWorker.register('service-worker.js');
    });
}